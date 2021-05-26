(ns glider.legacy.transaction.users
  (:require [clojure.data.xml :refer [emit-str]]
            [glider.legacy.transaction.xml :refer [parse-xml]]))

(comment (parse-xml "resources/user-basic-info.xml"))

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
      #_{:tag :elem,
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
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["Lookup_DS_fetch"]}]}
      #_{:tag :elem,
       :attrs {:xsi:type "xsd:Object"},
       :content
       [{:tag :criteria,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :type,
           :attrs nil,
           :content ["User role"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :attrs nil, :content ["Lookup_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}
          {:tag :textMatchStyle, :attrs nil, :content ["startsWith"]}]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["Lookup_DS_fetch"]}]}
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
         [{:tag :dataSource, :attrs nil, :content ["AddressDetail_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}
          {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
        {:tag :componentId, :attrs nil, :content ["isc_ListGrid_1"]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["fetchUserAddresses"]}]}
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
         [{:tag :dataSource, :attrs nil, :content ["ContactDetail_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}
          {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
        {:tag :componentId, :attrs nil, :content ["isc_ListGrid_2"]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation,
         :attrs nil,
         :content ["fetchContactByUserUid"]}]}]}]})

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

(defn all-users-transaction []
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
                          {:tag :startRow
                           :attrs {:xsi:type "xsd:long"}
                           :content ["0"]}
                          {:tag :endRow
                           :attrs {:xsi:type "xsd:long"}
                           :content ["1000"]}]}]}]})



