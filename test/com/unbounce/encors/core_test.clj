(ns com.unbounce.encors.core-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [com.unbounce.encors.types :refer [map->CorsPolicy]]
            [com.unbounce.encors.core :refer :all]))

(def ^:dynamic sut (atom nil))

(def http-port
  (let [configured-port
        (or (System/getProperty "test.http.port")
            (System/getenv "TEST_HTTP_PORT"))]
    (if configured-port
      (read-string configured-port)
      (+ 50000 (rand-int 15000)))))

(defn- uri
  ([]
    (uri ""))
  ([path]
    (format "http://localhost:%d%s" http-port path)))

(defn- handler [request]
  (@sut request))

(defroutes app
  (GET "/get" [] "got")
  (POST "/post" [] {:status 201 :body "posted"})
  (route/not-found "not found"))

;; Support

(def ^:const valid-origin "test.encors.net")
(def ^:const valid-methods "GET,POST")
(def ^:const valid-headers "Content-Type")
(def ^:const valid-credentials "true")

(def ^:const invalid-origin "not.cool.io")

(defn- assert-no-cors-response [res]
  (are [h] (nil? (->> res :headers h))
       :Access-Control-Allow-Origin
       :Access-Control-Allow-Methods
       :Access-Control-Allow-Headers
       :Access-Control-Allow-Credentials))

(defn- assert-full-cors-preflight-response [res]
  (are [h e] (= (->> res :headers h) e)
        :Access-Control-Allow-Origin valid-origin
        :Access-Control-Allow-Methods valid-methods
        :Access-Control-Allow-Headers valid-headers
        :Access-Control-Allow-Credentials valid-credentials))

(defn- assert-response [res expected-status cors-assertions-fn]
  (is (= (:status res) expected-status))
  (is (cors-assertions-fn res)))

;; App features tests

(defn- test-app-features [cors-assertions-fn]
  (testing "Application features"
    (let [get-res (http/get (uri "/get") {:throw-exceptions false})
          post-res (http/post (uri "/post") {:throw-exceptions false})
          root-res (http/get (uri "/") {:throw-exceptions false})]
      (assert-response get-res 200 cors-assertions-fn)
      (assert-response post-res 201 cors-assertions-fn)
      (assert-response root-res 404 cors-assertions-fn))))

(deftest app-features-no-cors
  (test-app-features assert-no-cors-response))

(deftest app-features-full-cors
  (test-app-features assert-full-cors-preflight-response))

;; CORS tests

(defn- send-preflight-request [origin]
         (http/options (uri "/")
                       {:headers {:Access-Control-Request-Method "POST"
                                  :Origin origin}
                        :throw-exceptions false}))

(deftest valid-preflight-no-cors
  (testing "Valid preflight"
    (let [res (send-preflight-request valid-origin)]
             (assert-response res 404 assert-no-cors-response))))

(deftest invalid-preflight-no-cors
  (testing "Invalid preflight"
    (let [res (send-preflight-request invalid-origin)]
             (assert-response res 404 assert-no-cors-response))))

(deftest valid-preflight-full-cors
  (testing "Valid preflight"
    (let [res (send-preflight-request valid-origin)]
             (assert-response res 200 assert-full-cors-preflight-response))))

(deftest invalid-preflight-full-cors
  (testing "Invalid preflight"
    (let [res (send-preflight-request invalid-origin)]
             (assert-response res 400 assert-no-cors-response))))

;; Jetty, tests and SUT orchestration

(defn test-ns-hook []
  (let [jetty (run-jetty handler {:join? false :port http-port})]

    ;; Test the raw app, without CORS middleware
    (testing "No CORS policy"
      (reset! sut app)
      (app-features-no-cors)
      (valid-preflight-no-cors)
      (invalid-preflight-no-cors))

    ;; Test the app, with CORS middleware (full config)
    (testing "Full CORS policy"
      (reset! sut app)
      (app-features-full-cors)
      (valid-preflight-full-cors)
      (invalid-preflight-full-cors))

    (.stop jetty)))

;; Unit tests for helper functions

(deftest cors-common-headers-test
  (let [cors-options
        {:allowed-origins nil
         :allowed-methods #{}
         :exposed-headers nil
         :request-headers #{}
         :max-age nil
         :allow-credentials? false
         :origin-varies? true
         :require-origin? true
         :ignore-failures? false}]

    (testing "when origin is not present when origin-varies? is true"
      (let [policy (map->CorsPolicy cors-options)]
        (is (= (cors-common-headers nil policy)
               {"Access-Control-Allow-Origin" "*"
                "Vary" "Origin"}))))

    (testing "when origin is not present and origin-varies? is false"
      (let [policy (map->CorsPolicy (merge cors-options
                                           {:origin-varies? false}))]
        (is (= (cors-common-headers nil policy)
               {"Access-Control-Allow-Origin" "*"}))))

    (testing "when origin is present and allow credentials is true"
      (let [origin "foobar.com"
            policy (map->CorsPolicy (merge cors-options {:allowed-origins #{origin}
                                                         :allow-credentials? true}))]
        (is (= (cors-common-headers origin policy)
               {"Access-Control-Allow-Origin" origin
                "Access-Control-Allow-Credentials" "true"}))))

    (testing "when origin is present and allow credentials is false"
      (let [origin "foobar.com"
            policy (map->CorsPolicy (merge cors-options
                                           {:allowed-origins #{origin}}))]
        (is (= (cors-common-headers origin policy)
               {"Access-Control-Allow-Origin" origin}))))))
