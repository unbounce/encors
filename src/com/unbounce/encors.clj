(ns com.unbounce.encors
  (:require [potemkin.namespaces :refer [import-vars]]))

(import-vars
 [com.unbounce.encors.types map->CorsPolicy]
 [com.unbounce.encors.core wrap-cors])
