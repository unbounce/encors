(defproject com.unbounce/encors "2.5.0-SNAPSHOT"
  :description "encors is a CORS library for ring"
  :url "https://www.github.com/unbounce/encors"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"
            :comments "Copyright (c) 2014-2020 Unbounce Marketing Solutions Inc."}

  :profiles
  {:dev {:plugins [[lein-kibit "0.1.8"]
                   [jonase/eastwood "0.3.6"]]
         :dependencies [[org.clojure/clojure "1.8.0"]
                        [clj-http "3.10.1"]
                        [ring/ring-jetty-adapter "1.8.1"]
                        [compojure "1.6.1"]
                        [manifold "0.1.8"]]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.1"]]}
   }

  :aliases {"all" ["with-profile" "dev,1.8:dev,1.9:dev,1.10:dev"]}

  ; set to true to have verbose debug of integration tests
  :jvm-opts ["-Ddebug=false"]

  :eastwood {:exclude-linters [:constant-test]}

  :dependencies [[potemkin "0.4.5"]
                 [org.clojure/core.match "1.0.0"]
                 [prismatic/schema "1.1.12"]]

  :deploy-repositories {"releases" {:url "https://repo.clojars.org" :creds :gpg}})
