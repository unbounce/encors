(ns com.unbounce.encors.aleph
  "CORS Middleware with aleph deferred support.

  This namespace requires you to have aleph/manifold on the classpath."
  (:require [manifold.deferred :as d]
            [com.unbounce.encors.core :as core]))

(defn wrap-cors
  [handler cors-policy]
  (core/build-cors-wrapper handler cors-policy
                           (fn -apply-deferred-headers [response headers]
                             (if (d/deferred? response)
                               (d/chain' response #(core/apply-ring-headers % headers))
                               (core/apply-ring-headers response headers)))))
