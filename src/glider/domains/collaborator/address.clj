(ns glider.domains.collaborator.address)

(def Schema
  [:map
   [::primary boolean?]
   [::supplied-date {:optional true} inst?]
   [::street-name string?]
   [::street-number string?]
   [::country-name string?]
   [::postcode string?]
   [::city string?]
   [::state string?]])
