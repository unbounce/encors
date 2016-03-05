(defproject com.unbounce/encors "2.2.2-SNAPSHOT"
  :description "encors is a CORS library for ring"
  :url "https://www.github.com/unbounce/encors"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2014-2016 Unbounce Marketing Solutions Inc."}

  :profiles {:dev {:plugins [[lein-kibit "0.1.2"]
                             [jonase/eastwood "0.2.3"]]
                   :dependencies [[clj-http "2.1.0"]
                                  [ring/ring-jetty-adapter "1.4.0"]
                                  [compojure "1.4.0"]]}}

  ; set to true to have verbose debug of integration tests
  :jvm-opts ["-Ddebug=false"]

  :eastwood {:exclude-namespaces [com.unbounce.encors]}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.match "0.2.2"]
                 [prismatic/schema "1.0.5"]
                 [potemkin "0.4.3"]])
