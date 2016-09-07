(def ring-version "1.4.0")

(defproject com.unbounce/encors "2.3.1-SNAPSHOT"
  :description "encors is a CORS library for ring"
  :url "https://www.github.com/unbounce/encors"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2014-2016 Unbounce Marketing Solutions Inc."}

  :profiles {:dev {:plugins [[lein-kibit "0.1.2"]
                             [jonase/eastwood "0.2.3"]]
                   :dependencies [[clj-http "3.2.0"]
                                  [ring/ring-jetty-adapter ~ring-version]
                                  [compojure "1.4.0"]
                                  [manifold "0.1.5"]]}}

  ; set to true to have verbose debug of integration tests
  :jvm-opts ["-Ddebug=false"]

  :eastwood {:exclude-linters [:constant-test]}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [potemkin "0.4.3"]
                 [org.clojure/core.match "0.2.2"]
                 [prismatic/schema "1.1.3"]
                 [ring/ring-core ~ring-version]])
