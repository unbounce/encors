(defproject com.unbounce/corse "0.1.0-SNAPSHOT"
  :description "corsé is a CORS library for ring"
  :url "https://www.github.com/unbounce/corse"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2014 Unbounce Marketing Solutions Inc."}
  
  :profiles {:dev {:plugins [[lein-kibit "0.0.8"][jonase/eastwood "0.1.4"]] 
                   :dependencies [[clj-http "1.0.1"]]}}

  :dependencies [[org.clojure/clojure "1.6.0"]])
