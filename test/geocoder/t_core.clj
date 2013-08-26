(ns geocoder.t-core
	(import com.google.code.geocoder.Geocoder)
	
  (:use midje.sweet)
  (:require [geocoder.core :as core]))

(def gc (new Geocoder))

(facts "a csv line should be interpreted by the --in-fields vector and then generate the appropriated map"
  (core/parse-in-fields ":name _ :age" ["Paulo" "Suzart" 20]) => {:name "Paulo" :age 20}
  (core/parse-in-fields ":name _ :age" ["Agustinho" "Coelho" "37"]) => {:name "Agustinho" :age "37"}
  (core/parse-in-fields ":age :gender" [21 "M"]) => {:age 21 :gender "M"}
  (core/parse-in-fields ":city :uf :street" ["Sao jose" "SP" 2 3]) => {:city "Sao jose", :street 2, :uf "SP"} ;;works with more field but less mappings
  (core/parse-in-fields ":id :street :number :bairro :locality :uf :cep :tel :fax :oa :categoria :site" ["()  -" "null" "null" "null" "null" "null" "null" "null" "null" "null" "null" "http://"]) =>
       {:bairro "null", :categoria "null", :cep "null", :fax "null", :id "()  -", :locality "null", :number "null", :oa "null", :site "http://", :street "null", :tel "null", :uf "null"}
)

(def default-query (core/prepared-line-parser ":street, :number, :locality - :uf, brasil"))


(facts "build the correct request"
  (let [req (core/build-req default-query {:bairro "null", :categoria "null", :cep "null", :fax "null", :id "()  -", :locality "null", :number "null", :oa "null", :site "http://", :street "null", :tel "null", :uf "null"})]
    (.getAddress req) => "null, null, null - null, brasil")

  (let [req (core/build-req default-query {:uf "BA" :locality "Salvador" :number "3172" :street "Av. Luis Viana Filho"})]
    (.getAddress req) => "Av. Luis Viana Filho, 3172, Salvador - BA, brasil"
    (.getLanguage req) => "pt_BR")

  (let [req (core/build-req default-query {:uf "BA" :locality "Salvador" :number "" :street "Av. Luis Viana Filho"})]
    (.getAddress req) => "Av. Luis Viana Filho, , Salvador - BA, brasil"
    (.getLanguage req) => "pt_BR"))

(facts "The outputer should be able to write anything"
  (let [out (core/prepared-line-parser "The name is :name. But age is\\: :age")]
    (out {:name "Paulo" :age "22"}) => "The name is Paulo. But age is\\: 22")

  (let [out (core/prepared-line-parser "<name>:name</name><age>:age</age>")]
    (out {:name "Paulo" :age "22"}) => "<name>Paulo</name><age>22</age>")

  (let [out (core/prepared-line-parser "UPDATE city set name = \":city\" where id = :id")]
    (out {:id 3213 :city "Salvador"}) => "UPDATE city set name = \"Salvador\" where id = 3213"))


(facts "Generate appropriate result from a geocode request"

     
  (let [o (core/prepared-line-parser ":id, :lat, :lng")]
       (core/gen-result o
         (core/geocode (core/goo-gcf gc) default-query
              {:bairro "null", :categoria "null", :cep "null", :fax "null", :id "()  -", :locality "null", :number "null", :oa "null", :site "http://", :street "null", :tel "null", :uf "null"}))=> "()  -, retry, retry")

  (let [o (core/prepared-line-parser ":id, :lat, :lng")]
       (core/gen-result o
         (core/geocode (core/goo-gcf gc) default-query
              {:uf "SP" :street "Alameda dos Nhambiquaras" :number "1645" :locality "São Paulo" :id "POI"})) => "POI, -23.6121401, -46.6641445")

 (let [o (core/prepared-line-parser ":id, :lat, :lng")]
       (core/gen-result o
         (core/geocode (core/goo-gcf gc) default-query
              {:uf "BA" :locality "Sal" :number "3" :street "null" :id "UNIFACS"})) => "UNIFACS, unavailable, unavailable")
       
  (let [o (core/prepared-line-parser ":zip :lat :lng")
        query (core/prepared-line-parser ":address")]
       (core/gen-result o
          (core/geocode (core/goo-gcf gc) query
              {:address "Alameda Santos, 2395 - 01419-002 - Jardim Paulista - São Paulo - SP"})) => "01419-002 -23.5696946 -46.649367")
       
  (let [o (core/prepared-line-parser ":zip :lat :lng")
        query (core/prepared-line-parser ":address")]
       (core/gen-result o
          (core/geocode (core/goo-gcf gc) query
              {:address "32, Carrera 78 # 2-35, Medellín, Antioquia, Colombia"})) => "unavailable 6.2126334 -75.60095070000001")

  (let [o (core/prepared-line-parser ":zip :lat :lng")
        query (core/prepared-line-parser ":address")]
       (core/gen-result o
          (core/geocode (core/goo-gcf gc) query
              {:address "Rua Boa Vista Brotas, 28, Engenho Velho de Brotas, Salvador - BA"})) => "40240-340 -12.9828452 -38.5000983"))
       


