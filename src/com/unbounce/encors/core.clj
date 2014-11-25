(ns com.unbounce.encors.core
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.core.match :refer [match]]
   [com.unbounce.encors.types :as types]))

(defn cors-failure [err-msg]
  { :status 400
    :headers {"Content-Type" "text/html; charset-utf-8"}
    :body err-msg })

;; Origin -> CorsPolicy -> Headers
(defn cors-common-headers [origin cors-policy]
  (match [origin (.allow-credentials? cors-policy) (.origin-varies? cors-policy)]
    [nil _ true]     {"Access-Control-Allow-Origin" "*" "Vary" "Origin"}
    [nil _ false]    {"Access-Control-Allow-Origin" "*"}

    [origin false _] {"Access-Control-Allow-Origin" origin}

    [origin true _]  {"Access-Control-Allow-Origin" origin
                      "Access-Control-Allow-Credentials" "true"}))

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-check-max-age
  [_ cors-policy]
  (if-let [max-age (.max-age cors-policy)]
    [:right {"Access-Control-Max-Age" (str max-age)}]
    ;; else
    [:right {}]))

;; Keyword -> String (uppercase)
(defn adapt-method-name [header-name]
  (-> header-name name str/upper-case))

;; #{Keyword} -> #{String}
(defn adapt-method-names [methods]
  (mapv adapt-method-name methods))

;; String -> #{String}
(defn adapt-req-headers-str [header-str]
  (set (mapv str/trim (str/split header-str #","))))

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-check-method
  [{:keys [headers] :as req} cors-policy]
  (let [cors-method (get headers "Access-Control-Request-Method")
        supported-methods (adapt-method-names
                           (set/union (.allowed-methods cors-policy)
                                      types/simple-methods))
        supported-methods-str (str/join ", " supported-methods)]
    (cond
     (nil? cors-method)
     [:left [(str "Access-Control-Request-Method header "
                  "is missing in CORS preflight request.")]]

     (not (contains? supported-methods cors-method))
     [:left [(str "Method requested in "
                     "Access-Control-Request-Method of CORS request "
                     "is not supported; requested: `" cors-method "'; "
                     "supported are " supported-methods-str ".")]]
     :else
     [:right {"Access-Control-Allow-Methods" supported-methods-str}])))

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-check-request-headers
  [{:keys [headers] :as req} cors-policy]
  (let [supported-headers (set/union (.request-headers cors-policy)
                                     types/simple-headers-wo-content-type)
        supported-headers-str (str/join ", " supported-headers)
        control-req-headers-str (get headers "Access-Control-Request-Headers")
        control-req-headers (adapt-req-headers-str control-req-headers-str)]

    (println (str "===> " supported-headers-str))
    (if (set/subset? control-req-headers supported-headers)
      [:right {"Access-Control-Allow-Headers" supported-headers}]
      [:left [(str "HTTP header requested in Access-Control-Request-Headers of "
                   "CORS request is not supported; requested: `" control-req-headers-str
                   "'; supported are `" supported-headers-str "'.")]])))

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-headers [req cors-policy]
  (let [mappend-either (fn merge-result [result1 result2]
                       (match [result1 result2]
                              [[:left a] [:left b]] [:left (concat a b)]
                              [[:right a] [:right b]] [:right (merge a b)]
                              [[:left _] _] result1
                              [_ [:left _]] result2))]
    (reduce (fn mconcat-either [result checker]
              (mappend-either result (checker req cors-policy)))
            [cors-preflight-check-max-age
             cors-preflight-check-method
             cors-preflight-check-request-headers])))

(defn apply-cors-policy [{:keys [req app origin cors-policy]}]
  (let [allowed-origins (.allowed-origins cors-policy)
        fail-or-ignore (fn fail [err-msg]
                         (if (.ignore-failures? cors-policy)
                           (app req)
                           (cors-failure err-msg)))]

    (cond
     ;;
     (and allowed-origins
          (contains? allowed-origins origin))
     ;;
     (if (= "OPTIONS" (get "method" req))
       (let [e-preflight-headers (cors-preflight-headers req cors-policy)]
         (match e-preflight-headers
                [:left err-msg] (fail-or-ignore err-msg)
                [:right preflight-headers]
                (let [common-headers (cors-common-headers origin cors-policy)
                      all-headers (merge common-headers preflight-headers)]
                  {:status 204
                   :headers all-headers
                   :body ""})))
       ;; else
       (fail-or-ignore "pending implementation"))

     ;;
     :else
     (fail-or-ignore "pending implementation"))))

(defn wrap-cors [get-policy-for-req app]
  (fn wrap-cors-handler [req]
    (let [cors-policy     (get-policy-for-req req)
          allowed-origins (.allowed-origins cors-policy)
          origin          (get (:headers req) "origin")]

      (cond
       ;; halt & fail
       (and origin (.require-origin? cors-policy))
       (cors-failure "Origin header is missing")

       ;; continue with inner app
       (not (.require-origin? cors-policy))
       (app req)

       :else ;; perform cors validation
       (apply-cors-policy {:req req
                           :app app
                           :origin origin
                           :cors-policy cors-policy})))))
