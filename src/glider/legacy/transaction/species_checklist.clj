(ns glider.legacy.transaction.species-checklist
  (:require 
    [malli.core :as m]
    [malli.error :as me]
    [malli.generator :as mg]
    [malli.transform :as mt]
    [camel-snake-kebab.core :as csk]
    [clj-http.client :refer [request] :as http]
    [clojure.data.csv :as csv]))

(def species-checklist-url
  "https://vba.dse.vic.gov.au/vba/downloadVSC.do")

(def species-checklist-request
  {:method :get
   :url species-checklist-url})

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map (comp keyword csk/->kebab-case))
            repeat)
       (rest csv-data)))
(comment
  (def species-checklist
    (-> species-checklist-request
        request
        :body
        csv/read-csv
        csv-data->maps))

  {:extract-date "20/12/2019",
   :restricted-flag "",
   :nvis-growthform "",
   :short-name "aca flav",
   :all-discipline-codes "[tf][ma][ai][af]",
   :parent-taxon-id "5015",
   :taxon-type "Fish",
   :taxon-level-cde "spec",
   :authority "(Temmninck & Schlegel, 1845)",
   :epbc-act-status "",
   :scientific-name "Acanthogobius flavimanus",
   :common-name "Yellowfin Goby",
   :vic-advisory-status "",
   :ffg-act-status "",
   :parent-taxon-level-cde "gen",
   :primary-discipline "Aquatic fauna",
   :origin "Introduced",
   :scientific-nme-synonym "Gobius flavimanus",
   :treaties "",
   :print-order-num "",
   :common-nme-synonym "Japanese Goby, Oriental Goby",
   :last-mod "20130916",
   :taxon-id "5016"}

  (->> (first species-checklist)
       keys
       (map vector))

  (def taxon-schema
    [:map
     [:extract-date inst?]; "20/12/2019"
     [:restricted-flag keyword?]; "rest" "breed"
     [:nvis-growthform string?]; "Tree-fern" "Mallee shrub" "Other grass" "Palm" "Vine" "Fern" "Forb" "Shrub" "Tussock grass" "Tree" "Epiphyte" "Hummock grass"
     [:short-name string?]
     [:all-discipline-codes
      {:decode/discipline
       '(fn [s]
          (->> (re-seq #"\[(.{2})\]" s)
               (mapv (comp keyword second))))}
      [:vector keyword?]]
     [:parent-taxon-id int?]
     [:taxon-type string?]
     [:taxon-level-cde keyword?]
     [:authority string?]
     [:epbc-act-status keyword?]; "Critically Endangered" "Endangered" "Vulnerable" "Extinct" "Near Threatened"
     [:scientific-name string?]
     [:common-name string?]
     [:vic-advisory-status string?]; "Data deficient" "Near threatened" "Critically endangered" "All infraspecific taxa included in Advisory List" "Regionally extinct" "Rare" "Poorly known" "Vulnerable" "Presumed extinct" "Extinct in the Wild" "Endangered"
     [:ffg-act-status string?];"Listed" "Rejected" "De-listed" "Nominated" "Invalid/ineligible/rejected"

     [:parent-taxon-level-cde keyword?];"phy" "supf" "trib" "gen" "ord" "subo" "div" "cla" "fam" "subc" "king" "supo" "subf" "subp"
     [:primary-discipline string?]; "Aquatic fauna" "Aquatic invertebrates" "Flora" "Marine" "Terrestrial fauna"
     [:origin string?] ; "Introduced" "Native but some stands may be alien" "Naturalised alien" "Introduced but doubt it ever established a population in victoria" "Doubt that it has ever been established in victoria"
     [:scientific-nme-synonym [:vector string?]]
     [:treaties string?] ;what's that ?
     [:print-order-num string?] ;to remove
     [:common-nme-synonym [:vector string?]]
     [:last-mod inst?]
     [:taxon-id int?]])

  

)
