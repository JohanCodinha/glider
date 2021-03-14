(ns glider.domains.collaborator.contact-method)

(def Schema
  [:multi
   {:dispatch :type}
   [:fax
    [:map
     [::type [:= :fax]]
     [::primary boolean?]
     [::number string?]]]
   [:phone
    [:map
     [::type [:= :phone]]
     [::primary boolean?]
     [::number string?]]]
   [:email
    [:map
     [::type [:= :email]]
     [::primary boolean?]
     [::address string?]]]])
