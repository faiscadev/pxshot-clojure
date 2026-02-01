(defproject com.pxshot/pxshot "0.1.0"
  :description "Official Clojure SDK for the Pxshot screenshot API"
  :url "https://github.com/pxshot/pxshot-clojure"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [cheshire "5.12.0"]]

  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}

  :source-paths ["src"]
  :test-paths ["test"]

  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]

  :scm {:name "git"
        :url "https://github.com/pxshot/pxshot-clojure"})
