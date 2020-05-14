(ns com.unbounce.encors.types
  (:require [schema.core :as s]))

(def match-origin :match-origin)

(def star-origin :star-origin)

(s/defschema CorsPolicySchema
  {(s/required-key :allowed-origins)    (s/cond-pre (s/enum match-origin star-origin)
                                                    #{s/Str})
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

(def access-control-request-headers-whitelist
  ;; Only Safari sends these access-control-request-headers on the preflight
  ;; even though they are considered "simple headers".
  ;; Note: these *must* be lowercase
  #{"origin" "accept"})
