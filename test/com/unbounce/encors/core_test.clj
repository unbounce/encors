(ns com.unbounce.encors.core-test
  (:require [clojure.test :refer :all]
            [clojure.set :as set]
            [com.unbounce.encors.types :refer [map->CorsPolicy]]
            [com.unbounce.encors.types :as types]
            [com.unbounce.encors.core :refer :all]))

(def default-cors-options
  {:allowed-origins nil
   :allowed-methods #{}
   :exposed-headers nil
   :request-headers #{}
   :max-age nil
   :allow-credentials? false
   :origin-varies? true
   :require-origin? true
   :ignore-failures? false})

(deftest cors-common-headers-test
  (testing "when origin is not present when origin-varies? is true"
    (let [policy (map->CorsPolicy default-cors-options)]
      (is (= (cors-common-headers nil policy)
             {"Access-Control-Allow-Origin" "*"
              "Vary" "Origin"}))))

  (testing "when origin is not present and origin-varies? is false"
    (let [policy (map->CorsPolicy (merge default-cors-options
                                         {:origin-varies? false}))]
      (is (= (cors-common-headers nil policy)
             {"Access-Control-Allow-Origin" "*"}))))

  (testing "when origin is present and allow credentials is true"
    (let [origin "foobar.com"
          policy (map->CorsPolicy (merge default-cors-options {:allowed-origins #{origin}
                                                       :allow-credentials? true}))]
      (is (= (cors-common-headers origin policy)
             {"Access-Control-Allow-Origin" origin
              "Access-Control-Allow-Credentials" "true"}))))

  (testing "when origin is present and allow credentials is false"
    (let [origin "foobar.com"
          policy (map->CorsPolicy (merge default-cors-options
                                         {:allowed-origins #{origin}}))]
      (is (= (cors-common-headers origin policy)
             {"Access-Control-Allow-Origin" origin})))))

(deftest cors-preflight-check-max-age-test
  (testing "when max-age is present"
    (let [max-age 365
          policy (map->CorsPolicy (merge default-cors-options
                                         {:max-age 365}))]
      (is (= (cors-preflight-check-max-age {:headers {}} policy)
             [:right {"Access-Control-Max-Age" (str max-age)}]))))
  (testing "when max-age is not present"
    (let [policy (map->CorsPolicy default-cors-options)]
      (is (= (cors-preflight-check-max-age {:headers {}} policy))
          [:right {}]))))

(deftest cors-preflight-check-method-test
  (testing "Access-Control-Request-Method header has a method allowed by the policy"
    (let [method :get
          policy (map->CorsPolicy default-cors-options)]
      (is (= (cors-preflight-check-method
              {:headers {"Access-Control-Request-Method" "GET"}}
              policy)
             [:right {"Access-Control-Allow-Methods" "HEAD, POST, GET"}]))))

  (testing "Access-Control-Request-Method header has a method not allowed by the policy"
    (let [method :delete
          policy (map->CorsPolicy default-cors-options)]
      (is (= (cors-preflight-check-method
              {:headers {"Access-Control-Request-Method" "DELETE"}}
              policy)
             [:left [(str "Method requested in Access-Control-Request-Method of "
                           "CORS request is not supported; requested: `DELETE'; "
                           "supported are HEAD, POST, GET.")]]))))

  (testing "Access-Control-Request-Method header is missing"
    (let [method :get
          policy (map->CorsPolicy default-cors-options)]
      (is (= (cors-preflight-check-method {:headers {}} policy)
             [:left [(str "Access-Control-Request-Method header is missing in CORS "
                           "preflight request.")]])))))

(deftest cors-preflight-check-request-headers-test
  (let [policy (map->CorsPolicy (merge default-cors-options
                                       {:request-headers #{"X-Safe-To-Expose"}}))]
    (testing "Access-Control-Request-Headers doesn't match policy request headers"
      (is (= (cors-preflight-check-request-headers
              {:headers {"Access-Control-Request-Headers"
                         "X-Not-Safe-To-Expose, X-Blah-Bleh"}}
              policy)
             [:left [(str "HTTP header requested in Access-Control-Request-Headers of "
                          "CORS request is not supported; requested: "
                          "`X-Not-Safe-To-Expose, X-Blah-Bleh'; "
                          "supported are `X-Safe-To-Expose, Accept-Language, "
                          "Content-Language, Accept'.")]])))
    (testing "Access-Control-Request-Headers match policy request headers"
      (is (= (cors-preflight-check-request-headers
              {:headers {"Access-Control-Request-Headers"
                         "X-Safe-To-Expose"}}
              policy)
             [:right {"Access-Control-Allow-Headers"
                      (set/union #{"X-Safe-To-Expose"}
                                 types/simple-headers-wo-content-type)}])))))

(deftest cors-preflight-headers-test
  (testing "With a request that complies with policy"
    (let [policy (map->CorsPolicy (merge default-cors-options
                                         {:max-age 365
                                          :allow-credentials? true
                                          :request-headers #{"X-Cool-Header"}
                                          :allowed-methods #{:get}}))]
      (is (= (cors-preflight-headers {:headers {"Access-Control-Request-Headers"
                                                "X-Cool-Header"
                                                "Access-Control-Request-Method"
                                                "GET"}}
                                     policy)
             [:right {"Access-Control-Allow-Headers"
                      (set/union #{"X-Cool-Header"}
                                 types/simple-headers-wo-content-type)
                      "Access-Control-Allow-Methods" "HEAD, POST, GET"
                      "Access-Control-Max-Age" "365"}])))))
