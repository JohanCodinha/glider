(ns glider.domain.collaborator.contact-method
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]
            [malli.generator :as mg]))

(def Schema
   [:or
    [:map
     [:contact-method/type
      [:enum "Fax Number" "Mobile Phone" "Home Phone" "Work Phone"]]
     [:contact-method/primary boolean?]
     [:contact-method/number string?]]
    [:map
     [:contact-method/type [:= "Email Address"]]
     [:contact-method/primary boolean?]
     [:contact-method/address string?]]])
