(defproject geocoder "ultimate-geo"
  :description "gets the right street name"
  :url "http://paulosuzart.github.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
				         [com.google.code.geocoder-java/geocoder-java "0.15"]
                 [clojure-csv/clojure-csv "2.0.0-alpha1"]
                 [org.clojure/tools.cli "0.2.2"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}}
  :main geocoder.core)
