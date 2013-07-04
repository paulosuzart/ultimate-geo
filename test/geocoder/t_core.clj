(ns geocoder.t-core
	(import com.google.code.geocoder.Geocoder)
	
  (:use midje.sweet)
  (:require [geocoder.core :as core]))

(def gc (new Geocoder))

(facts "a csv line should be interpreted by the --in-fields vector and then generate the appropriated map"
  (core/to-geodata '[:name _ :age] ["Paulo" "Suzart" 20]) => {:name "Paulo" :age 20}
  (core/to-geodata '[:name _ :age] ["Agustinho" "Coelho" "37"]) => {:name "Agustinho" :age "37"}
  (core/to-geodata '[:age :gender] [21 "M"]) => {:age 21 :gender "M"}
  (core/to-geodata '[:city :uf :street] ["Sao jose" "SP" 2 3]) => {:city "Sao jose", :street 2, :uf "SP"} ;;works with more field but less mappings
  (core/to-geodata '[:id :street :number :bairro :locality :uf :cep :tel :fax :oa :categoria :site] ["()  -" "null" "null" "null" "null" "null" "null" "null" "null" "null" "null" "http://"]) =>
       {:bairro "null", :categoria "null", :cep "null", :fax "null", :id "()  -", :locality "null", :number "null", :oa "null", :site "http://", :street "null", :tel "null", :uf "null"}
)

(facts "build the correct request"
  (let [req (core/build-req {:bairro "null", :categoria "null", :cep "null", :fax "null", :id "()  -", :locality "null", :number "null", :oa "null", :site "http://", :street "null", :tel "null", :uf "null"})]
    (.getAddress req) => "null, null, null - null, brasil")

  (let [req (core/build-req {:uf "BA" :locality "Salvador" :number "3172" :street "Av. Luis Viana Filho"})]
    (.getAddress req) => "Av. Luis Viana Filho, 3172, Salvador - BA, brasil"
    (.getLanguage req) => "pt_BR")

  (let [req (core/build-req {:uf "BA" :locality "Salvador" :number "" :street "Av. Luis Viana Filho"})]
    (.getAddress req) => "Av. Luis Viana Filho, , Salvador - BA, brasil"
    (.getLanguage req) => "pt_BR"))

(facts "The outputer should be able to write anything"
  (let [out (core/outputer ["The name is %s. But age is %s" [:name :age]])]
    (out {:name "Paulo" :age "22"}) => "The name is Paulo. But age is 22")

  (let [out (core/outputer ["<name>%s</name><age>%s</age>" [:name :age]])]
    (out {:name "Paulo" :age "22"}) => "<name>Paulo</name><age>22</age>")

  (let [out (core/outputer ["UPDATE city set name = \"%s\" where id = %s" [:city :id]])]
    (out {:id 3213 :city "Salvador"}) => "UPDATE city set name = \"Salvador\" where id = 3213"))


(facts "Generate appropriate result from a geocode request"
  (let [o (core/outputer ["%s, %s, %s" [:id :lat :lng]])]
       (core/gen-result o
         (core/geocode (core/goo-gcf gc)
              {:bairro "null", :categoria "null", :cep "null", :fax "null", :id "()  -", :locality "null", :number "null", :oa "null", :site "http://", :street "null", :tel "null", :uf "null"}))=> "UNIFACS, -12.94566060, -38.42104860")


  (let [o (core/outputer ["%s, %s, %s" [:id :lat :lng]])]
       (core/gen-result o
         (core/geocode (core/goo-gcf gc)
              {:uf "BA" :locality "Salvador" :number "3172" :street "Av. Luis Viana Filho" :id "UNIFACS"})) => "UNIFACS, -12.94566060, -38.42104860")

  (let [o (core/outputer ["%s, %s, %s, %s" [:id :lat :lng :street]])]
       (core/gen-result o
         (core/geocode (core/goo-gcf gc)
              {:uf "BA" :locality "Salvador" :number "3172" :street "Av. Luis Viana Filho" :id "UNIFACS"})) => "UNIFACS, -12.94566060, -38.42104860, Av. Luis Viana Filho")
  (let [o (core/outputer ["%s, %s, %s" [:id :lat :lng]])]
       (core/gen-result o
         (core/geocode (core/goo-gcf gc)
              {:uf "BA" :locality "Sal" :number "3" :street "null" :id "UNIFACS"})) => "UNIFACS, unavailable, unavailable"))


