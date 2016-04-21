(defproject ciste/ciste "0.6.0-SNAPSHOT"
  :description "MVC platform for Clojure applications"
  :url "http://github.com/duck1123/ciste"
  :author "Daniel E. Renfer <duck@kronkltd.net>"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[clj-factory "0.2.1"]
                 [clj-http "2.1.0"]
                 [clojurewerkz/eep "1.0.0-beta1"]
                 [clojurewerkz/propertied "1.2.0"]
                 [com.taoensso/timbre "4.3.1"]
                 [compojure "1.5.0"]
                 [enlive "1.1.6"]
                 [environ "1.0.2"]
                 [inflections "0.12.0" :exclusions [commons-codec]]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 [xom "1.2.5"]]
  :profiles {:dev {:dependencies [[log4j "1.2.17"]
                                  [midje "1.8.3"]
                                  [org.slf4j/slf4j-log4j12 "1.7.18"]]}}
  :auto-clean false
  :aot [ciste.runner]
  :plugins [[lein-midje "3.1.3"]
            [codox "0.8.11"]
            [lein-ancient "0.6.7"]
            [lein-bikeshed "0.2.0"]
            [lein-environ "1.0.0"]]
  :repositories [["snapshots" {:url "http://artifactory.jiksnu.org/artifactory/libs-snapshot-local/"
                               :username [:gpg :env/artifactory_username]
                               :password [:gpg :env/artifactory_password]}]
                 ["releases" {:url "http://artifactory.jiksnu.org/artifactory/libs-releases-local/"
                              :username [:gpg :env/artifactory_username]
                              :password [:gpg :env/artifactory_password]}]]
  :autodoc {:name "Ciste"
            :copyright "2014 KRONK Ltd."})
