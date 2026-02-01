(ns build
  "Build script for Pxshot Clojure SDK.

  Usage:
    clj -T:build clean
    clj -T:build jar
    clj -T:build deploy"
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'com.pxshot/pxshot)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean
  "Clean build artifacts."
  [_]
  (b/delete {:path "target"}))

(defn jar
  "Build the JAR file."
  [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]
                :scm       {:url "https://github.com/pxshot/pxshot-clojure"
                            :connection "scm:git:git://github.com/pxshot/pxshot-clojure.git"
                            :developerConnection "scm:git:ssh://git@github.com/pxshot/pxshot-clojure.git"
                            :tag (str "v" version)}
                :pom-data  [[:description "Official Clojure SDK for the Pxshot screenshot API"]
                            [:url "https://pxshot.com"]
                            [:licenses
                             [:license
                              [:name "MIT"]
                              [:url "https://opensource.org/licenses/MIT"]]]]})
  (b/copy-dir {:src-dirs   ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))

(defn deploy
  "Deploy to Clojars.

  Requires CLOJARS_USERNAME and CLOJARS_PASSWORD environment variables."
  [_]
  (jar nil)
  (dd/deploy {:installer :remote
              :artifact  jar-file
              :pom-file  (b/pom-path {:lib lib :class-dir class-dir})}))
