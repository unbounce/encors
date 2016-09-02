(ns com.unbounce.encors.aleph-test
  (:require [clojure.test :refer :all]
            [com.unbounce.encors.aleph :as sut]
            [manifold.deferred :as d]))

(deftest wrap-cors-aleph-test
  (let [cors-policy       {:allowed-origins    #{"foo"}
                           :allowed-methods    #{:get}
                           :exposed-headers    #{"some-header"}
                           :request-headers    #{}
                           :max-age            nil
                           :allow-credentials? false
                           :origin-varies?     true
                           :require-origin?    true
                           :ignore-failures?   false}
        expected-response {:status  200
                           :headers {"Access-Control-Allow-Origin"   "foo"
                                     "Access-Control-Expose-Headers" "some-header"}
                           :body    "ok"}]
    (testing "applies response headers correctly"
      (testing "synchronous response"
        (let [handler (sut/wrap-cors (constantly {:status 200 :body "ok"}) cors-policy)]
          (is (= expected-response
                 (handler {:method :get :headers {"origin" "foo"}})))))
      (testing "deferred response"
        (let [handler  (sut/wrap-cors (constantly (d/future {:status 200 :body "ok"})) cors-policy)
              response (handler {:method :get :headers {"origin" "foo"}})]
          (is (d/deferred? response))
          (is (= expected-response
                 @response)))))))
