(ns geocoder.core
	(import com.google.code.geocoder.Geocoder)
	(import com.google.code.geocoder.GeocoderRequestBuilder)
	(import com.google.code.geocoder.model.LatLng)
 	(import com.google.code.geocoder.model.GeocoderStatus)
 	(import com.google.code.geocoder.model.GeocoderAddressComponent)
 	(import com.google.code.geocoder.model.GeocoderResultType)
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.tools.cli :only [cli]]
        [clojure.java.io :only [reader writer]]
        [clojure.string :only [replace]]
  (:refer-clojure :exclude [replace])
  (:gen-class))

(defn build-req
  "Builds google API request"
  [parse-query address]
	(.getGeocoderRequest
     (doto (new GeocoderRequestBuilder)
      (.setAddress (parse-query address))
      (.setLanguage "pt_BR"))))

(defn goo-gcf
  "Geocode function. Wraps .geocode"
  [gc]
  (fn [req] (.geocode gc req)))

(defn- gzip
  "Gets the zip code from the results"
  [^GeocoderAddressComponent component]
  (.getShortName component))


(defn- glat 
  "Gets the latitude from result"
  [result]
  (.getLat (.getLocation (.getGeometry result))))

(defn- glng [result]
  "Gets the longitude from results"
  (.getLng (.getLocation (.getGeometry result))))

(defn is-useful
	"Checks if a given GeocodeResponde or AddresComponent are useful"
  [types result]
  (let [useful (some types (.getTypes result))]
      useful))

(def is-useful-component (partial is-useful #{"postal_code"}))

(def is-useful-result (partial is-useful #{"street_address" "route"}))                                     


(defn gen-result
  "Google returns sometimes lots of results for the same query.
  This function will find the GeocoderResult that owns `stree_address` types and
  then grab its data"
  [outputter [response in-data]]
  (if (= "OK" (.value (.getStatus response)))
    (let [results (.getResults response)
          geo (transient {:lat "unavailable" :lng "unavailable" :zip "unavailable"})]

      (when-let [valid-result (first (filter is-useful-result results))]
        (assoc! geo :lat (glat valid-result) :lng (glng valid-result))
        
        (when-let [valid-component (first (filter is-useful-component (.getAddressComponents valid-result)))]
          (assoc! geo :zip (gzip valid-component))))

      (outputter (merge in-data (persistent! geo))))

    (outputter (merge in-data {:lat "retry" :lng "retry" :zip "retry"}))))

(defn geocode
	"Geocode function itsel. Builds a request and return back the 
  result and processin gitem tuple."
  [gcf q i]
  (Thread/sleep (-> (rand-int 3) (+ 1) (* 1000))) ;; google often drop serial requests
  (let [req (build-req q i)
        response (gcf req)]
       [response i]))

(defn parse-in-fields
	"Converts a CSV line to a map"
  [fields line]

  (let [fields- (map second (re-seq #"(?<!\\):(\w+)|\_" fields))
        mapper-full (map-indexed #(vector %1 (keyword %2)) fields-)
        mapper-valid (filter second  mapper-full)]
    (apply merge (map #(hash-map (second %) (nth line (first %))) mapper-valid))))

(defn write-output
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

(defn process-input [opts]
  (println opts)
  (let [{:keys [source
               target
               delimiter
               maps-query
               out-format
               in-fields]} opts]
     (->> (parse-csv (reader source) :delimiter delimiter)
          (pmap #(parse-in-fields in-fields %))
          (pmap #(geocode (goo-gcf (new Geocoder)) (prepared-line-parser maps-query) %))
          (pmap #(gen-result (prepared-line-parser out-format) %))
          (write-output target))
     (System/exit 0)))


(defn -main
  [& args]
	(let [
        [opts args banner]
             (cli args ["-in" "--in-fields" "A string that describes how to mapp each parsed field: \"_ :city :name setreet _ _ :id\""]
                       ["-out" "--out-format" "A string that be written to the output file. :lat and :lng are also available: \":id, :lat, :ln\""]
                       ["-query" "--maps-query" "The query that will be actually submitted to google maps. `-in` used if not supplied"]
                       ["-d" "--delimiter" "A csv delimiter. Defaults to ," :default \, :parse-fn #(first  %)]
                       ["-h" "--help" "Show this help." :default false :flag true]
                       ["-t" "--target" "Target file." :default "./geo-target"]
                       ["-s" "--source" "Target file." :default "./geo-source"]
                  )]
      (when (:help opts)
        (println banner)
        (System/exit 0))
      (process-input
        (if (not (:maps-query opts))
            (assoc opts :maps-query (:in-fields opts))
            opts))
 ))
