(ns glider.domain.collaborator.collaborator
  (:require [glider.domain.collaborator.address :as address]
            [glider.domain.collaborator.contact-method :as contact-method]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.generator :as mg]
            [malli.core :as malli]
            [malli.transform :as mt]
            [glider.utils :as utils]
            [java-time :as time]
            [malli.error :as me]
            [malli.registry :as mr]))

;;Data model
(def Schema
  [:map
   [:account-creation-date {:decode/inst utils/timestamp} :inst]
   [:status {:default "Not Approved"}
    [:enum
     "Active"
     "Inactive"
     "New user"
     "Deceased user"
     "Deleted"
     "Not Approved"]]
   [:legacy-Uid {:optional true} int?]
   [:entity/uuid :uuid]
   [:given-name string?]
   [:surname string?]
   [:login-name string?]
   [:other-name {:optional true} string?]
   [:reason-of-use
    [:or [:enum
          "DATA ENTRY - amateur naturalist"
          "DATA ENTRY - ecologist (employed or researcher)"
          "DATA ENTRY - admin for data provider"
          "QUERY & RESEARCH - environmental planner"
          "QUERY & RESEARCH - ecologist (consultancy or land manager)"
          "QUERY & RESEARCH - researcher"
          "QUERY & RESEARCH - general interest & education"]
     string?]]
   [:role {:default "View only"}
    [:enum
     "Expert Reviewer"
     "View only"
     "Contributor"
     "Taxon Manager"
     "Administrator"]]
   [:batch-upload-access {:default false} :boolean]
   [:restricted-viewing-access {:default false} :boolean]
   [:terms-and-conditions-accepted-date {:optional true} inst?]
   #_[:contacts [:+ contact-method/Schema]]
   #_[:addresses [:+ address/Schema]]])

(def registry
  (mr/composite-registry
   m/default-registry
   {:inst (m/-simple-schema
           {:type :inst
            :pred inst?})}))

(defn parse [input]
  (m/decode
   Schema
   input
   (mt/string-transformer)))

(defn coerce [user-info]
  (m/decode
   Schema
   user-info
   {:registry registry}
   (mt/transformer
    {:name :inst}
    (mt/strip-extra-keys-transformer)
    (mt/string-transformer)
    (mt/default-value-transformer
     {:defaults {:inst (constantly (utils/timestamp))
                 :uuid (constantly (utils/uuid))}}))))

#_(mu/optional-keys Schema {:registry registry})
(mg/generate Schema  {:registry registry})
;;create contact
;;create address ?
;;create collaborator

(defn map->nsmap
  [m n]
  (into
   {}
   (map
    (fn [[k v]]
      (let [new-kw (if (and (keyword? k)
                            (not (qualified-keyword? k)))
                     (keyword (name n) (name k))
                     k)]
        [new-kw v]))
    m)))

(defn collaborator [user-info]
  (let [coerced (coerce user-info)
        valid? (m/validate Schema coerced {:registry registry})
        striped-keys (filterv #(when-not (contains? coerced %) %) (keys user-info))]
    (cond
      (seq striped-keys) {:error {:striped-keys striped-keys}
                          :coerced coerced}
      valid? (map->nsmap coerced :collaborator)
      :else {:error (me/humanize (m/explain Schema coerced {:registry registry}))
             :coerced coerced})))



(let [coerced
      (collaborator
       {:login-name "JohanPhoto"
        :surname "Codinha"
        :given-name "Johan"
        :reason-of-use "DATA ENTRY - amateur naturalist"})]
  coerced)



(filter #(when-not (contains? {:a 1 :b 2 :c 3 :f 3} %)
        %) [:c])

(def register-collaborator-inputs
  [:map
   [:given-name string?]
   [:surname string?]
   [:login-name string?]
   [:other-name {:optional true} string?]
   [:reason-of-use string?]
   #_[:email string?]
   #_[:password string?]])
