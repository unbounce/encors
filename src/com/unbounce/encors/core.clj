(ns com.unbounce.encors.core
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.core.match :refer [match]]
   [schema.core :as s]
   [com.unbounce.encors.types :as types]))

(defn cors-failure [err-msg]
  { :status 400
    :headers {"Content-Type" "text/html"}
    :body (if (vector? err-msg) (first err-msg) err-msg)})

;; Origin -> CorsPolicy -> Headers
(defn cors-common-headers [origin cors-policy]
  (if (or (nil? origin)
          (= (:allowed-origins cors-policy) types/star-origin))
    ;; ^ At this point we already validated all cases where either
    ;; of this two being nil is an *error*
    (merge {"Access-Control-Allow-Origin" "*"}
           (if (:origin-varies? cors-policy)
             {"Vary" "Origin"}
             {}))
    ;; else
    (merge {"Access-Control-Allow-Origin" origin}
           (if (:allow-credentials? cors-policy)
             {"Access-Control-Allow-Credentials" "true"}
             {}))))

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-check-max-age
  [_ cors-policy]
  (if-let [max-age (:max-age cors-policy)]
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
(defn header-string-to-set [header-str]
  (if (empty? header-str)
    #{}
    (into
      (sorted-set)
      (mapv
        (comp str/lower-case str/trim)
        (str/split header-str #",")))))

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-check-method
  [{:keys [headers] :as req} cors-policy]
  (let [cors-method (get headers "access-control-request-method")
        supported-methods (into (sorted-set)
                                (-> (:allowed-methods cors-policy)
                                  (set/union types/simple-methods)
                                  adapt-method-names))

        supported-methods-str (str/join ", " supported-methods)]
    (cond
     (nil? cors-method)
     [:left [(str "Access-Control-Request-Method header "
                  "is missing in CORS preflight request.")]]

     (not (contains? supported-methods cors-method))
     [:left [(str "Method requested in "
                  "Access-Control-Request-Method of CORS request "
                  "is not supported; requested: '" cors-method "'; "
                  "supported are " supported-methods-str ".")]]
     :else
     [:right {"Access-Control-Allow-Methods" supported-methods-str}])))

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-check-request-headers
  [{:keys [headers] :as req} cors-policy]
  (let [supported-headers (:request-headers cors-policy)
        supported-headers* (into
                             (sorted-set)
                             (map str/lower-case supported-headers))
        supported-headers-str (str/join ", " supported-headers)
        control-req-headers-str (get headers "access-control-request-headers")
        control-req-headers (set/difference
                              (header-string-to-set control-req-headers-str)
                              types/access-control-request-headers-whitelist)]

    (if (or (empty? control-req-headers)
            (set/subset? control-req-headers supported-headers*))
      [:right {"Access-Control-Allow-Headers" supported-headers-str}]
      [:left [(str "HTTP headers requested in Access-Control-Request-Headers of "
                   "CORS request is not supported; requested: '" control-req-headers-str
                   "'; supported are '" supported-headers-str "'.")]])))

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
            [:right {}]
            [cors-preflight-check-max-age
             cors-preflight-check-method
             cors-preflight-check-request-headers])))

(defn apply-ring-headers [response headers]
  (update-in response [:headers] merge headers))

(defn apply-cors-policy [{:keys [req app origin cors-policy apply-headers]}]
  (let [allowed-origins (:allowed-origins cors-policy)
        common-headers (cors-common-headers origin cors-policy)
        fail-or-ignore (fn fail [err-msg]
                         (if (:ignore-failures? cors-policy)
                           (app req)
                           (cors-failure err-msg)))]

    (cond
      ;;
      (or
       (= allowed-origins types/match-origin)
       (= allowed-origins types/star-origin)
       (contains? allowed-origins origin))

      ;; check if it is a preflight request
      (if (= :options (get req :request-method))
        (let [e-preflight-headers (cors-preflight-headers req cors-policy)]
          (match e-preflight-headers
                 [:left err-msg] (fail-or-ignore err-msg)
                 [:right preflight-headers]
                 {:status 204
                  :headers (merge common-headers preflight-headers)
                  :body ""}))
        ;; else
        (let [control-expose-headers
              (if-let [exposed-headers (:exposed-headers cors-policy)]
                {"Access-Control-Expose-Headers" (str/join ", " exposed-headers)}
                {})

              all-headers
              (merge common-headers control-expose-headers)]
          (apply-headers (app req) all-headers)))

      ;;
      :else
      (fail-or-ignore (str "Unsupported origin: " (pr-str origin))))))

(defn build-cors-wrapper
  [app cors-policy apply-headers]
  (s/validate types/CorsPolicySchema cors-policy)
  (fn wrap-cors-handler [req]
    (let [origin (get (:headers req) "origin")]

      (cond
        ;; halt & fail
        (and (nil? origin)
             (:require-origin? cors-policy))
        (cors-failure "Origin header is missing")

        ;; continue with inner app
        (nil? origin)
        (app req)

        :else ;; perform cors validation
        (apply-cors-policy {:req req
                            :app app
                            :apply-headers apply-headers
                            :origin origin
                            :cors-policy cors-policy})))))

(defn wrap-cors
  "CORS Middleware for standard ring applications.

  See com.unbounce.encors.types for schema definition of cors-policy
  "
  [handler cors-policy]
  (build-cors-wrapper handler cors-policy apply-ring-headers))
