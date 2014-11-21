(def ring-version "1.3.1")

(defproject com.unbounce/encors "0.1.0-SNAPSHOT"
  :description "encors is a CORS library for ring"
  :url "https://www.github.com/unbounce/encors"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2014 Unbounce Marketing Solutions Inc."}

  :profiles {:dev {:plugins [[lein-kibit "0.0.8"]
                             [jonase/eastwood "0.1.4"]]
                   :dependencies [[clj-http "1.0.1"]
                                  [ring/ring-jetty-adapter ~ring-version]
                                  [compojure "1.2.1"]]}}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.2.1"]
                 [prismatic/schema "0.3.3"]
                 [ring/ring-core ~ring-version]])
