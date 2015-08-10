(ns com.unbounce.encors.types
  (:require [schema.core :as s]))

(def match-origin :match-origin)

(def star-origin :star-origin)

(def CorsPolicySchema
  {(s/required-key :allowed-origins)    (s/either #{s/Str}
                                                  (s/enum match-origin star-origin))
   (s/required-key :allowed-methods)    #{(s/enum :head :options :get
                                                  :post :put :delete :patch :trace)}
   (s/required-key :request-headers)    #{s/Str}
   (s/required-key :exposed-headers)    (s/maybe #{s/Str})
   (s/required-key :max-age)            (s/maybe s/Int)
   (s/required-key :allow-credentials?) s/Bool
   (s/required-key :origin-varies?)     s/Bool
   (s/required-key :require-origin?)    s/Bool
   (s/required-key :ignore-failures?)   s/Bool})


(def simple-methods #{:get :head :post})

(def simple-headers #{"Accept" "Accept-Language" "Content-Language" "Content-Type" "Origin"})

(def simple-headers-wo-content-type #{"Accept" "Accept-Language" "Content-Language" "Origin"})
