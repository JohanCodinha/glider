(ns glider.domains.legacy.wrapper.users
  (:require #_[clj-http.client :refer [request] :as http]
            [glider.domains.legacy.wrapper.xml :refer [parse-xml]]
            [clojure.data.xml :refer [emit-str]]
            [glider.domains.legacy.wrapper.utils
             :refer [fetch-rows!]
             :as utils]))
(parse-xml "resources/user-basic-info.xml")
(defn user-details [userUid]
  {:tag :transaction,
   :attrs
   {:xsi:type "xsd:Object",
    :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"},
   :content
   [{:tag :operations,
     :attrs {:xsi:type "xsd:List"},
     :content
     [{:tag :elem,
       :attrs {:xsi:type "xsd:Object"},
       :content
       [{:tag :criteria,
         :attrs {:xsi:type "xsd:Object"},
         :content [{:tag :userUid, :attrs nil, :content [userUid]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :attrs nil, :content ["UserInfoView_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}]}

        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["userInfoById"]}]}
      {:tag :elem,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :criteria,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :USER_UID,
             :attrs {:xsi:type "xsd:long"},
             :content [userUid]}]}
          {:tag :operationConfig,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :dataSource,
             :attrs nil,
             :content ["UserOrganisationLink_DS"]}
            {:tag :operationType, :attrs nil, :content ["fetch"]}
            {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
          {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
          {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
          {:tag :sortBy,
           :attrs {:xsi:type "xsd:List"},
           :content
           [{:tag :elem, :attrs nil, :content ["organisationId"]}]}
          {:tag :componentId, :attrs nil, :content ["isc_ListGrid_0"]}
          {:tag :appID, :attrs nil, :content ["builtinApplication"]}
          {:tag :operation,
           :attrs nil,
           :content ["UserOrganisationLink_DS_fetch"]}
          {:tag :oldValues,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :USER_UID,
             :attrs {:xsi:type "xsd:long"},
             :content ["10660"]}]}]}
      #_{:tag :elem,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :criteria,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :USER_UID,
             :attrs {:xsi:type "xsd:long"},
             :content ["10660"]}]}
          {:tag :operationConfig,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :dataSource, :attrs nil, :content ["AddressDetail_DS"]}
            {:tag :operationType, :attrs nil, :content ["fetch"]}
            {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
          {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
          {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
          {:tag :sortBy,
           :attrs {:xsi:type "xsd:List"},
           :content
           [{:tag :elem, :attrs nil, :content ["mainAddressId"]}]}
          {:tag :componentId, :attrs nil, :content ["isc_ListGrid_1"]}
          {:tag :appID, :attrs nil, :content ["builtinApplication"]}
          {:tag :operation, :attrs nil, :content ["fetchUserAddresses"]}
          {:tag :oldValues,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :USER_UID,
             :attrs {:xsi:type "xsd:long"},
             :content ["10660"]}]}]}
      #_{:tag :elem,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :criteria,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :USER_UID,
             :attrs {:xsi:type "xsd:long"},
             :content ["10660"]}]}
          {:tag :operationConfig,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :dataSource, :attrs nil, :content ["ContactDetail_DS"]}
            {:tag :operationType, :attrs nil, :content ["fetch"]}
            {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
          {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
          {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
          {:tag :sortBy,
           :attrs {:xsi:type "xsd:List"},
           :content
           [{:tag :elem, :attrs nil, :content ["primaryContactId"]}]}
          {:tag :componentId, :attrs nil, :content ["isc_ListGrid_2"]}
          {:tag :appID, :attrs nil, :content ["builtinApplication"]}
          {:tag :operation,
           :attrs nil,
           :content ["fetchContactByUserUid"]}
          {:tag :oldValues,
           :attrs {:xsi:type "xsd:Object"},
           :content
           [{:tag :USER_UID,
             :attrs {:xsi:type "xsd:long"},
             :content ["10660"]}]}]}]}]})
(def user-preferred-contact-method
  {:tag :transaction,
   :attrs
   {:xsi:type "xsd:Object",
    :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"},
   :content
   [{:tag :transactionNum,
     :attrs {:xsi:type "xsd:long"},
     :content ["8"]}
    {:tag :operations,
     :attrs {:xsi:type "xsd:List"},
     :content
     [{:tag :elem,
       :attrs {:xsi:type "xsd:Object"},
       :content
       [{:tag :criteria,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :type,
           :attrs nil,
           :content ["Preferred Contact Method"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :attrs nil, :content ["Lookup_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}
          {:tag :textMatchStyle, :attrs nil, :content ["startsWith"]}]}
        {:tag :componentId,
         :attrs nil,
         :content ["isc_PickListMenu_2"]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["Lookup_DS_fetch"]}
        {:tag :oldValues,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :type,
           :attrs nil,
           :content ["Preferred Contact Method"]}]}]}]}]})
(def active-users-transaction 
  {:tag :transaction
   :attrs
   {:xsi:type "xsd:Object"
    :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"}
   :content
   [{:tag :operations
     :content [{:tag :elem
                :attrs {:xsi:type "xsd:Object"}
                :content [{:tag :criteria
                           :attrs {:xsi:type "xsd:Object"}
                           :content [{:tag :statusCde
                                      :content ["active"]}]
                           }
                          {:tag :operationConfig
                           :attrs {:xsi:type "xsd:Object"}
                           :content [{:tag :dataSource
                                      :content ["UserInfoShowView_DS"]}
                                     {:tag :operationType
                                      :content ["fetch"]}
                                     {:tag :textMatchStyle
                                      :content ["exact"]}]}
                          {:tag :componentId
                           :content ["isc_ManageUserAdminModule$2_0"]}
                          {:tag :appID
                           :content ["builtinApplication"]}
                          {:tag :operation
                           :content ["userInfoMainSearch"]}
                          {:tag :oldValues
                           :attrs {:xsi:type "xsd:Object"}
                           :content [{:tag :statusCde
                                      :content ["active"]}]}]}]}]})

(def all-users-transaction 
  {:tag :transaction
   :attrs
   {:xsi:type "xsd:Object"
    :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"}
   :content
   [{:tag :operations
     :attrs {:xsi:type "xsd:List"}
     :content [{:tag :elem
                :attrs {:xsi:type "xsd:Object"}
                :content [{:tag :criteria
                           :attrs {:xsi:type "xsd:Object"}
                           :content []}
                          {:tag :operationConfig
                           :attrs {:xsi:type "xsd:Object"}
                           :content [{:tag :dataSource
                                      :content ["UserInfoShowView_DS"]}
                                     {:tag :operationType
                                      :content ["fetch"]}
                                     {:tag :textMatchStyle
                                      :content ["exact"]}]}
                          {:tag :componentId
                           :content ["isc_ManageUserAdminModule$2_0"]}
                          {:tag :appID
                           :content ["builtinApplication"]}
                          {:tag :operation
                           :content ["userInfoMainSearch"]}
                          {:tag :oldValues
                           :attrs {:xsi:type "xsd:Object"}
                           :content []}]}]}]})

(defn get-all-users!
  "Fetch all users, return a lazy seq"
  [cookie]
  (fetch-rows! all-users-transaction 100 cookie))

