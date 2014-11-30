(ns com.unbounce.encors
  (:require [potemkin.namespaces :refer [import-vars]]
            [com.unbounce.encors.core]))

(import-vars
  [com.unbounce.encors.core wrap-cors])
