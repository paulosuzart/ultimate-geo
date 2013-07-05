;; Autor: Paulo Suzart
;; This code reads a CSV file with a given format and then
;; talks to google to get its route name and generage a SQL output
;; just lein run it. No args to the main.

(ns geocoder.core
	(import com.google.code.geocoder.Geocoder)
	(import com.google.code.geocoder.GeocoderRequestBuilder)
	(import com.google.code.geocoder.model.LatLng)
  	(import com.google.code.geocoder.model.GeocoderStatus)
  	(import com.google.code.geocoder.model.GeocoderResultType)
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.tools.cli :only [cli]]
        [clojure.java.io :only [reader writer]]
        [clojure.string :only [replace]]
        [clojure.algo.monads :only [domonad maybe-m]])
  (:refer-clojure :exclude [replace])
  (:gen-class))

(defn build-req
  "Builds google API request"
  [parse-query address]
	(.getGeocoderRequest
     (doto (new GeocoderRequestBuilder)
      ;(.setLocation (LatLng. (:lat address) (:lng address)))
      (.setAddress (parse-query address))
      (.setLanguage "pt_BR"))))

(defn goo-gcf
  "Geocode function. Wraps .geocode"
  [gc]
  (fn [req] (.geocode gc req)))

(defn glat [result]
  (.getLat (.getLocation (.getGeometry result))))

(defn glng [result]
  (.getLng (.getLocation (.getGeometry result))))

(defn is-useful
	"Checks if a given GeocodeResponde or AddresComponent are useful"
  [t]
    (let [useful (some #{"street_address" "route"} (.getTypes t))]
      useful))

(defn gen-result
	"Given a GeocodeResponse g and a processing item i, emits
  the needed SQL (gen-sql) or information about the failure.
  Failures are written as commented SQL statements
  "
  [o [response i]]
  (if (= "OK" (.value (.getStatus response)))
    (or
      (domonad maybe-m
         [results (.getResults response)
		      valid-result (first (filter is-useful results))
		      valid-component (first (filter is-useful (.getAddressComponents valid-result)))
          lat (glat valid-result)
          lng (glng valid-result)]
            (o (merge i {:lat lat :lng lng})))
      (o (merge i {:lat "unavailable" :lng "unavailable"})) ; (format "--Unable to find useful information for %s\n" (:id i))})))
    (o (merge i {:lat "retry" :lng "retry"}))))) ;(format "--Unable to geocode %s due to %s\n" (:id i) (.value (.getStatus response)))}))))

(defn geocode
	"Geocode function itsel. Builds a request and return back the 
  result and processin gitem tuple."
  [gcf q i]
  (Thread/sleep (-> (rand-int 3) (+ 1) (* 1000))) ;; google often drop serial requests
  (let [req (build-req q i)
        response (gcf req)]
       [response i]))

(defn to-geodata
	"Converts a CSV line to a map"
  [fields line]

  (let [fields- (map second (re-seq #"(?<!\\):(\w+)|\_" fields))
        mapper-full (map-indexed #(vector %1 (keyword %2)) fields-)
        mapper-valid (filter second  mapper-full)]
    (apply merge (map #(hash-map (second %) (nth line (first %))) mapper-valid))))

(defn write-output
	"Takes the seq of geocoded data and writes to a predefined file. updates.sql"
  [target content]
  (with-open [w (writer target)]
    (doseq [i content]
      (if i (do
        (println "Writing " i)
        (.write w i)
        (.newLine w))))))

(defn prepared-line-parser [^String line]
  (fn [variables]
      (loop [out-keys (map second (re-seq #"(?<!\\):(\w+)" line)) output line]
       (if (seq out-keys)
         (let [current-key (first out-keys)
               replace-val (str ((keyword current-key) variables))
               replace-str (re-pattern (str ":" current-key))]
          (recur (rest out-keys) (replace output replace-str replace-val)))
         output))))

(defn -main
  [& args]
	(let [gc (new Geocoder)
        [opts args banner]
             (cli args ["-in" "--in-fields" "A string that describes how to mapp each parsed field: \"_ :city :name setreet _ _ :id\""]
                       ["-out" "--out-format" "A string that be written to the output file. :lat and :lng are also available: \":id, :lat, :ln\""]
                       ["-query" "--maps-query" "The query that will be actually submitted to google maps"]
                       ["-d" "--delimiter" "A csv delimiter. Defaults to ," :default \, :parse-fn #(first  %)]
                       ["-h" "--help" "Show this help." :default false :flag true]
                       ["-t" "--target" "Target file." :default "./geo-target"]
                       ["-s" "--source" "Target file." :default "./geo-source"]
                  )]
      (when (:help opts)
        (println banner)
        (System/exit 0))
      (->> (parse-csv (reader (:source opts)) :delimiter (:delimiter opts))
           (pmap #(to-geodata (:in-fields opts) %))
           (pmap #(geocode (goo-gcf gc) (prepared-line-parser (:maps-query opts)) %))
           (pmap #(gen-result (prepared-line-parser (:out-format opts)) %))
           (write-output (:target opts)))["-h" "--help" "Show this help." :default false :flag true])
   (System/exit 0))
