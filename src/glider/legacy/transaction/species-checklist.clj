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

(def species-checklist
  (-> species-checklist-request
      request
      :body
      csv/read-csv
      csv-data->maps))

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map (comp keyword csk/->kebab-case))
            repeat)
       (rest csv-data)))

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

(->>(first species-checklist)
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
        (->>(re-seq #"\[(.{2})\]" s)
            (mapv (comp keyword second))))}
    [:vector keyword?]]; "[af][ma]" "[fl][tf][ai][af]" "[ai][ma][tf][af]" "[af]" "[tf][af][ai][ma]" "[af][ma][tf][ai]" "[fl][af][tf]" "[ma][af]" "[ai][ma]" "[ma][af][ai]" "[ai][tf][af]" "[ai][tf][af][fl]" "[tf][ai][af][ma]" "[af][ai][ma][tf]" "[af][ma][ai]" "[ai]" "[tf][fl][ai][af]" "[tf][ma][af][ai]" "[tf]" "[tf][ma]" "[ai][ma][af][tf]" "[ai][af][tf]" "[ai][fl][tf][af]" "[af][ai][ma]" "[ai][af][tf][ma]" "[ai][af][ma]" "[tf][ai]" "[af][ma][ai][tf]" "[af][fl]" "[ai][tf][fl]" "[af][tf][ai][fl]" "[af][tf][ai][ma]" "[ma][fl]" "[ma]" "[ai][af][tf][fl]" "[ai][tf][fl][af]" "[tf][ma][ai][af]" "[af][tf][ma]" "[ma][tf][ai][af]" "[tf][ai][af]" "[fl]" "[ma][ai]" "[tf][af][ma][ai]" "[fl][ma]" "[af][ai][tf]" "[fl][af]" "[tf][ai][ma][af]" "[tf][af][fl][ai]" "[ma][ai][tf][af]" "[af][ai]" "[ai][tf][ma][af]" "[ma][ai][af]" "[ma][ai][af][tf]" "[af][tf]" "[tf][ma][fl][ai][af]" "[ma][tf]" "[ma][af][tf][ai]" "[ma][af][ai][tf]" "[af][fl][ai][tf][ma]" "[tf][ai][af][fl]" "[fl][ai][tf][af]" "[ma][tf][af]" "[af][ma][tf]" "[ma][af][tf]" "[af][tf][ma][ai]" "[ma][tf][af][ai]" "[ma][tf][ai][fl][af]" "[ai][af][ma][tf]" "[ai][ma][af]" "[tf][af][ma]" "[tf][af]" "[ai][af]" "[af][tf][fl][ai]" "[ai][tf][af][ma]" "[tf][ma][af]" "[ai][fl][af][tf]" "[af][ai][tf][ma]" "[ma][af][ai][tf][fl]" "[ai][tf]" "[tf][af][ai]" "[af][tf][ai]"
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

  

(keys (frequencies (filter #(not= "" %) (map :extract-date species-checklist))))

(m/decode taxon-schema (first species-checklist)
          {:name :discipline})


(m/decode
  [:map
   [:restricted-flag keyword?]; "rest" "breed"
   [:nvis-growthform string?]; "Tree-fern" "Mallee shrub" "Other grass" "Palm" "Vine" "Fern" "Forb" "Shrub" "Tussock grass" "Tree" "Epiphyte" "Hummock grass"
   [:all-discipline-codes
    {:decode/discipline
     '(fn [s]
        (println s)
        (->>(re-seq #"\[(.{2})\]" s)
                    (mapv (comp keyword second))))}
    [:vector keyword?]]
   ]
  {
   :restricted-flag ""
   :nvis-growthform ""
   :all-discipline-codes "[tf][ma][ai][af]"
   }
  (mt/transformer {:name :discipline}))
o


