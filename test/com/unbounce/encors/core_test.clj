(ns com.unbounce.encors.core-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [ring.adapter.jetty :refer [run-jetty]]))

(def ^:dynamic sut (atom nil))

(def http-port
  (let [configured-port
        (or (System/getProperty "test.http.port")
            (System/getenv "TEST_HTTP_PORT"))]
    (if configured-port
      (read-string configured-port)
      (+ 50000 (rand-int 15000)))))

(defn- handler [request]
  (@sut request))

(defn- with-server [f]
  (let [jetty (run-jetty handler {:join? false :port http-port})]
    (f)
    (.stop jetty)))

(use-fixtures :once with-server)

(deftest no-cors-policy
  (is true))
