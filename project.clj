(def ring-version "1.3.2")

(defproject com.unbounce/encors "2.1.1-SNAPSHOT"
  :description "encors is a CORS library for ring"
  :url "https://www.github.com/unbounce/encors"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2014-2015 Unbounce Marketing Solutions Inc."}

  :profiles {:dev {:plugins [[lein-kibit "0.0.8"]
                             [jonase/eastwood "0.2.1"]]
                   :dependencies [[clj-http "1.1.2"]
                                  [ring/ring-jetty-adapter ~ring-version]
                                  [compojure "1.3.4"]]}}

  ; set to true to have verbose debug of integration tests
  :jvm-opts ["-Ddebug=false"]

  :eastwood {:exclude-namespaces [com.unbounce.encors]}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [potemkin "0.3.13"]
                 [org.clojure/core.match "0.2.2"]
                 [prismatic/schema "0.4.3"]
                 [ring/ring-core ~ring-version]])
