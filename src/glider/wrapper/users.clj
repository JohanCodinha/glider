(ns glider.wrapper.users
  (:require [clj-http.client :refer [request] :as http]
            [clojure.data.xml :refer [emit-str]]
            [glider.wrapper.utils
             :refer [http-post-request process-request]
             :as utils]))

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
  (utils/fetch-rows! all-users-transaction 100 cookie))

