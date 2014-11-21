(ns com.unbounce.encors.types
  (:require [schema.core :as s]))


(deftype CorsPolicy
    [allowed-origins
     allowed-methods
     request-headers
     exposed-headers
     max-age
     allow-credentials?
     origin-varies?
     ignore-failures?])

(def CorsPolicySchema
  {(s/required-key :allowed-origins)    (s/maybe [s/Str])
   (s/required-key :allowed-methods)    [(s/enum [:get :post :put :options])]
   (s/required-key :request-headers)    [s/Str]
   (s/required-key :exposed-headers)    (s/maybe [s/Str])
   (s/required-key :max-age)            (s/maybe s/Int)
   (s/required-key :allow-credentials?) s/Bool
   (s/required-key :origin-varies?)     s/Bool
   (s/required-key :ignore-failures?)   s/Bool})


(defn map->CorsPolicy [args]
  (s/validate CorsPolicySchema args)
  (CorsPolicy. (:allowed-origins args)
               (:allowed-methods args)
               (:request-headers args)
               (:exposed-headers args)
               (:max-age args)
               (:allow-credentials? args)
               (:origin-varies? args)
               (:ignore-failures? args)))

(defn create-cors-policy [& args-list]
  (let [args (merge {:allow-credentials? true
                     :ignore-failures? false}
                    (apply hash-map args-list))]
    (map->CorsPolicy args)))
