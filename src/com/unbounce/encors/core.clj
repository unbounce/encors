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

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-check-method
  [{:keys [headers] :as req} cors-policy]
  (let [supported-methods (set/union (.allowed-methods cors-policy)
                                     types/simple-methods)]
    (if-let [cors-method (get headers "Access-Control-Request-Method")]
      (let [supported-methods-value (str/join ", " (mapv name supported-methods))]
        (if (contains? supported-methods (keyword cors-method))
          [:right {"Access-Control-Allow-Methods" supported-methods-value}]
          ;; else
          [:left [(str "Method requested in "
                        "Access-Control-Request-Method of CORS request "
                        "is not supported; requested: " cors-method "; "
                        "supported are " supported-methods-value)]]))
      ;; else
      [:left [(str "Access-Control-Request-Method header "
                    "is missing in CORS preflight request")]])))

;; ;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
;; (defn cors-preflight-check-request-headers
;;   [{:keys [headers] :as req} cors-policy]
;;   (let [supported-headers (merge (.request-headers cors-policy)
;;                                  (types/simple-headers-wo-content-type))]
;;     (if-let [control-headers (get headers "Access-Control-Request-Headers")]
;;       ;; else
;;       [:right {}]
;;       ))
;;   )

;; Request -> CorsPolicy -> [:left [ErrorMsg]] | [:right Headers]
(defn cors-preflight-headers [req cors-policy]
  (let [merge-results (fn merge-result [result1 result2]
                       (match [result1 result2]
                              [[:left a] [:left b]] [:left (concat a b)]
                              [[:right a] [:right b]] [:right (merge a b)]
                              [[:left _] _] result1
                              [_ [:left _]] result2))]
    (reduce (fn merge-results-reduction [result checker]
              (merge-results result (checker req cors-policy)))
            [cors-preflight-check-max-age
             cors-preflight-check-method
             ;; cors-preflight-check-request-headers
             ])))
