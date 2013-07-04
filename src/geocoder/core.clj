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
        [clojure.algo.monads :only [domonad maybe-m]])
  (:gen-class))

(defn build-req
  "Builds google API request"
  [address]
	(.getGeocoderRequest 
     (doto (new GeocoderRequestBuilder)
      ;(.setLocation (LatLng. (:lat address) (:lng address)))
      (.setAddress (format "%s, %s, %s - %s, brasil" (:street address) (:number address) (:locality address) (:uf address)))
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
  [gcf i]
  (Thread/sleep (-> (rand-int 3) (+ 1) (* 1000))) ;; google often drop serial requests
  (let [req (build-req i)
        response (gcf req)]
       [response i]))

(defn to-geodata
	"Converts a CSV line to a map"
  [fields line]

  (let [mapper-full (map-indexed vector fields)
        mapper-valid (filter #(not (= '_ (second %))) mapper-full)]
    (apply merge (map #(hash-map (second %) (nth line (first %))) mapper-valid))))

(defn write-output
	"Takes the seq of geocoded data and writes to a predefined file. updates.sql"
  [target content]
  (with-open [w (writer target)]
    (doseq [i content]
      (if i (do
        (println "Writing " i)
        (.write w i))))))

(defn outputer [o]
  (fn [result]
    (if (:err result)
      (:err result)
      (apply format (first o) (flatten (map #(get result %) (second o)))))))

(defn -main
  [& args]
	(let [gc (new Geocoder)
        [opts args banner]
             (cli args ["-i" "--in-fields" "A map that describes how to thread each parsed field: [_ :city :name setreet _ _ :id]." :parse-fn #(read-string %)]
                       ["-o" "--out-format" "A map that will be written to a new csv file appended with lat and lng: [\"%s,%s\" [:id :lat :lng]" :parse-fn #(read-string %)]
                       ["-d" "--delimiter" "A csv delimiter. Defaults to ," :default \, :parse-fn #(first (read-string %))]
                       ["-t" "--target" "Target file." :default "./geo-target"]
                       ["-s" "--source" "Target file." :default "./geo-source"]
                  )]
       (println opts)
      (->> (parse-csv (reader (:source opts)) :delimiter (:delimiter opts))
           (pmap #(to-geodata (:in-fields opts) %))
           (pmap #(geocode (goo-gcf gc) %))
           (pmap #(gen-result (outputer (:out-format opts)) %))
           (write-output (:target opts))))
   (System/exit 0))
