(ns com.unbounce.encors.core-test
  (:require [clojure.test :refer :all]
            [clojure.set :as set]
            [clojure.string :as str]
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
    (let [policy default-cors-options]
      (is (= (cors-common-headers nil policy)
             {"Access-Control-Allow-Origin" "*"
              "Vary" "Origin"}))))

  (testing "when origin is not present and origin-varies? is false"
    (let [policy (merge default-cors-options
                        {:origin-varies? false})]
      (is (= (cors-common-headers nil policy)
             {"Access-Control-Allow-Origin" "*"}))))

  (testing "when origin is present and allow credentials is true"
    (let [origin "foobar.com"
          policy (merge default-cors-options {:allowed-origins #{origin}
                                              :allow-credentials? true})]
      (is (= (cors-common-headers origin policy)
             {"Access-Control-Allow-Origin" origin
              "Access-Control-Allow-Credentials" "true"}))))

  (testing "when origin is present and allow credentials is false"
    (let [origin "foobar.com"
          policy (merge default-cors-options
                        {:allowed-origins #{origin}})]
      (is (= (cors-common-headers origin policy)
             {"Access-Control-Allow-Origin" origin})))))

(deftest cors-preflight-check-max-age-test
  (testing "when max-age is present"
    (let [max-age 365
          policy (merge default-cors-options
                        {:max-age 365})]
      (is (= (cors-preflight-check-max-age {:headers {}} policy)
             [:right {"Access-Control-Max-Age" (str max-age)}]))))
  (testing "when max-age is not present"
    (let [policy default-cors-options]
      (is (= (cors-preflight-check-max-age {:headers {}} policy)
             [:right {}])))))

(deftest cors-preflight-check-method-test
  (testing "Access-Control-Request-Method header has a method allowed by the policy"
    (let [method :get
          policy default-cors-options]
      (is (= (cors-preflight-check-method
              {:headers {"access-control-request-method" "GET"}}
              policy)
             [:right {"Access-Control-Allow-Methods" "GET, HEAD, POST"}]))))

  (testing "Access-Control-Request-Method header has a method not allowed by the policy"
    (let [method :delete
          policy default-cors-options]
      (is (= (cors-preflight-check-method
              {:headers {"access-control-request-method" "DELETE"}}
              policy)
             [:left [(str "Method requested in Access-Control-Request-Method of "
                           "CORS request is not supported; requested: 'DELETE'; "
                           "supported are GET, HEAD, POST.")]]))))

  (testing "Access-Control-Request-Method header is missing"
    (let [method :get
          policy default-cors-options]
      (is (= (cors-preflight-check-method {:headers {}} policy)
             [:left [(str "Access-Control-Request-Method header is missing in CORS "
                           "preflight request.")]])))))

(deftest cors-preflight-check-request-headers-test
  (let [policy (merge default-cors-options
                      {:request-headers #{"X-Safe-To-Expose"}})]
    (testing "Access-Control-Request-Headers doesn't match policy request headers"
      (is (= (cors-preflight-check-request-headers
              {:headers {"access-control-request-headers"
                         "X-Not-Safe-To-Expose, X-Blah-Bleh"}}
              policy)
             [:left [(str "HTTP headers requested in Access-Control-Request-Headers of "
                          "CORS request is not supported; requested: "
                          "'X-Not-Safe-To-Expose, X-Blah-Bleh'; "
                          "supported are 'X-Safe-To-Expose, Accept-Language, "
                          "Content-Language, Accept'.")]])))
    (testing "Access-Control-Request-Headers match policy request headers"
      (is (= (cors-preflight-check-request-headers
              {:headers {"access-control-request-headers"
                         "X-Safe-To-Expose"}}
              policy)
             [:right {"Access-Control-Allow-Headers"
                      (str/join ", "
                        (set/union #{"X-Safe-To-Expose"}
                                   types/simple-headers-wo-content-type))}])))))

(deftest cors-preflight-headers-test
  (testing "With a request that complies with policy"
    (let [policy (merge default-cors-options
                        {:max-age 365
                         :allow-credentials? true
                         :request-headers #{"X-Cool-Header"}
                         :allowed-methods #{:get}})]
      (is (= (cors-preflight-headers {:headers {"access-control-request-headers"
                                                "X-Cool-Header"
                                                "access-control-request-method"
                                                "GET"}}
                                     policy)
             [:right {"Access-Control-Allow-Headers"
                      (str/join ", "
                        (set/union #{"X-Cool-Header"}
                                   types/simple-headers-wo-content-type))
                      "Access-Control-Allow-Methods" "GET, HEAD, POST"
                      "Access-Control-Max-Age" "365"}])))))

(deftest apply-cors-policy-test
  (testing ":allowed-origins has a :star-origin value"
    (let [policy (merge default-cors-options
                        {:allowed-origins types/star-origin
                         :origin-varies? false})
          response (apply-cors-policy {:req {}
                                       :app (constantly {:status 200 :headers {} :body "test is alright"})
                                       :origin nil
                                       :cors-policy policy})]
      (is (= (:status response) 200))
      (is (= (:headers response) {"Access-Control-Allow-Origin" "*"}))
      (is (= (:body response) "test is alright"))))
  (testing ":allowed-origins has a :star-origin value"
    (let [policy (merge default-cors-options
                        {:allowed-origins types/star-origin
                         :origin-varies? false})
          response (apply-cors-policy {:req {}
                                       :app (constantly {:status 200 :headers {} :body "test is alright"})
                                       :origin "http://foobar.com"
                                       :cors-policy policy})]
      (is (= (:status response) 200))
      (is (= (:headers response) {"Access-Control-Allow-Origin" "*"}))
      (is (= (:body response) "test is alright"))))
  (testing ":allowed-origins has a :match-origin value"
    (let [policy (merge default-cors-options
                        {:allowed-origins types/match-origin
                         :origin-varies? false})
          response (apply-cors-policy {:req {}
                                       :app (constantly {:status 200 :headers {} :body "test is alright"})
                                       :origin "http://foobar.com"
                                       :cors-policy policy})]
      (is (= (:status response) 200))
      (is (= (:headers response) {"Access-Control-Allow-Origin" "http://foobar.com"}))
      (is (= (:body response) "test is alright")))))
