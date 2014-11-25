(ns com.unbounce.encors.integration-test
  (:require [com.unbounce.encors :as cors]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [clj-http.client :as http]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route]))

(def debug
  (read-string
    (System/getProperty "debug" "false")))

(def ^:dynamic sut (atom nil))

(def http-port
  (let [configured-port
        (or (System/getProperty "http.port")
            (System/getenv "HTTP_PORT"))]
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

(defn- debug-res [req-uri res]
  (when debug
    (println "====> DEBUG for " req-uri ": \n"
             "\n"
             "- " (:status res) "\n"
             "- " (pr-str (:headers res)) "\n"
             "- " (:body res))))

;; Support

(def ^:const allowed-origin  "test.encors.net")
(def ^:const allowed-methods "GET, HEAD, PATCH, POST")
(def ^:const allowed-headers "Content-Type, X-Allowed")
(def ^:const active-allowed-headers "Accept-Language, Content-Language, Content-Type, X-Allowed, Accept")
(def ^:const expose-headers  "X-Safe-To-Expose, X-Safe-To-Expose-Too")
(def ^:const unallowed-origin "not.cool.io")

(def partial-cors-options
  {:allowed-origins #{allowed-origin}
   :allowed-methods (set (mapv (comp keyword str/lower-case)
                               (str/split allowed-methods #", ")))
   :exposed-headers nil
   :request-headers #{allowed-headers}
   :max-age nil
   :allow-credentials? false
   :origin-varies? true
   :require-origin? false
   :ignore-failures? false})

(def partial-cors-policy
  (cors/map->CorsPolicy partial-cors-options))

(def full-cors-options
  (merge partial-cors-options
         {:exposed-headers (set (str/split expose-headers #", "))
          :max-age 1234
          :allow-credentials? true
          :require-origin? true}))

(def full-cors-policy
  (cors/map->CorsPolicy full-cors-options))

(defn- assert-no-cors-response [res]
  (are [header] (nil? (-> res :headers (get header)))
       "Access-Control-Allow-Origin"
       "Access-Control-Allow-Methods"
       "Access-Control-Allow-Headers"
       "Access-Control-Allow-Credentials"
       "Access-Control-Expose-Headers"
       "Access-Control-Max-Age"))

(defn- assert-partial-cors-preflight-response [res]
  (are [header expected] (= (-> res :headers (get header)) expected)
        "Access-Control-Allow-Origin"      allowed-origin
        "Access-Control-Allow-Methods"     allowed-methods
        "Access-Control-Allow-Headers"     active-allowed-headers
        "Access-Control-Allow-Credentials" nil
        "Access-Control-Expose-Headers"    nil
        "Access-Control-Max-Age"           nil))

(defn- assert-full-cors-preflight-response [res]
  (are [header expected] (= (-> res :headers (get header)) expected)
        "Access-Control-Allow-Origin"      allowed-origin
        "Access-Control-Allow-Methods"     allowed-methods
        "Access-Control-Allow-Headers"     active-allowed-headers
        "Access-Control-Allow-Credentials" "true"
        "Access-Control-Expose-Headers"    nil
        "Access-Control-Max-Age"           "1234"))

(defn- assert-partial-cors-response [res]
  (are [header expected] (= (-> res :headers (get header)) expected)
        "Access-Control-Allow-Origin"      allowed-origin
        "Access-Control-Allow-Methods"     nil
        "Access-Control-Allow-Headers"     nil
        "Access-Control-Allow-Credentials" nil
        "Access-Control-Expose-Headers"    nil
        "Access-Control-Max-Age"           nil))

(defn- assert-full-cors-response [res]
  (are [header expected] (= (-> res :headers (get header)) expected)
        "Access-Control-Allow-Origin"      allowed-origin
        "Access-Control-Allow-Methods"     nil
        "Access-Control-Allow-Headers"     nil
        "Access-Control-Allow-Credentials" "true"
        "Access-Control-Expose-Headers"    expose-headers
        "Access-Control-Max-Age"           nil))

(defn- assert-response [res expected-status cors-assertions-fn]
  (is (= (:status res) expected-status))
  (is (cors-assertions-fn res)))

;; App features tests

(defn- test-app-features [origin expected-statuses cors-assertions-fn]
  (testing
    (str "Application features, Origin: "
         (case origin allowed-origin "Allowed" unallowed-origin "Unallowed" "nil"))
    (let [headers (if origin {:Origin origin} {})
          get-res (http/get (uri "/get")
                            {:headers headers
                             :throw-exceptions false})
          post-res (http/post (uri "/post")
                              {:headers (assoc headers :Content-Type "application/json")
                               :body "{\"data\":\"fake\"}"
                               :throw-exceptions false})
          root-res (http/get (uri "/")
                             {:headers headers
                              :throw-exceptions false})]

      (debug-res "get-res" get-res)
      (assert-response get-res (nth expected-statuses 0) cors-assertions-fn)

      (debug-res "post-res" get-res)
      (assert-response post-res (nth expected-statuses 1) cors-assertions-fn)

      (debug-res "root-res" get-res)
      (assert-response root-res (nth expected-statuses 2) cors-assertions-fn))))

(deftest app-features-no-cors
  (test-app-features nil [200 201 404] assert-no-cors-response)
  (test-app-features allowed-origin [200 201 404] assert-no-cors-response)
  (test-app-features unallowed-origin [200 201 404] assert-no-cors-response))

(deftest app-features-partial-cors
  (test-app-features nil [200 201 404] assert-no-cors-response)
  (test-app-features allowed-origin [200 201 404] assert-partial-cors-response)
  (test-app-features unallowed-origin [400 400 400] assert-no-cors-response))

(deftest app-features-full-cors
  (test-app-features nil [400 400 400] assert-no-cors-response)
  (test-app-features allowed-origin [200 201 404] assert-full-cors-response)
  (test-app-features unallowed-origin [400 400 400] assert-no-cors-response))

;; CORS tests

(defn- send-preflight-request [origin]
  (let [res (http/options (uri "/")
                          {:headers {:Access-Control-Request-Method "POST"
                                     :Origin origin}
                           :throw-exceptions false})]
    (debug-res "options /" res)
    res))

; TODO test invalid preflight with bad request method and bad request headers

(deftest valid-preflight-no-cors
  (testing "Valid preflight"
    (let [res (send-preflight-request allowed-origin)]
             (assert-response res 404 assert-no-cors-response))))

(deftest invalid-preflight-no-cors
  (testing "Invalid preflight"
    (let [res (send-preflight-request unallowed-origin)]
             (assert-response res 404 assert-no-cors-response))))

(deftest valid-preflight-partial-cors
  (testing "Valid preflight"
    (let [res (send-preflight-request allowed-origin)]
             (assert-response res 204 assert-partial-cors-preflight-response))))

(deftest invalid-preflight-partial-cors
  (testing "Invalid preflight"
    (let [res (send-preflight-request unallowed-origin)]
             (assert-response res 400 assert-no-cors-response))))

(deftest valid-preflight-full-cors
  (testing "Valid preflight"
    (let [res (send-preflight-request allowed-origin)]
             (assert-response res 204 assert-full-cors-preflight-response))))

(deftest invalid-preflight-full-cors
  (testing "Invalid preflight"
    (let [res (send-preflight-request unallowed-origin)]
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

    ;; Test the app, with CORS middleware (partial config)
    (testing "Partial CORS policy"
      (reset! sut
              (cors/wrap-cors (constantly partial-cors-policy) app))
      (app-features-partial-cors)
      (valid-preflight-partial-cors)
      (invalid-preflight-partial-cors))

    ;; Test the app, with CORS middleware (full config)
    (testing "Full CORS policy"
      (reset! sut
              (cors/wrap-cors (constantly full-cors-policy) app))
      (app-features-full-cors)
      (valid-preflight-full-cors)
      (invalid-preflight-full-cors))

    (.stop jetty)))
