(defproject geocoder "ultimate-geo"
  :description "Takes any .csv input, geocodes it and writes to any output file"
  :url "http://paulosuzart.github.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
				         [com.google.code.geocoder-java/geocoder-java "0.16"]
                 [clojure-csv/clojure-csv "2.0.2"]
                 [org.clojure/tools.cli "0.2.2"]]
  :profiles {:dev {:dependencies [[midje "1.9.0"]]}}
  :main geocoder.core)
