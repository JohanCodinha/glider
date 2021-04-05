(ns glider.legacy.transaction.project
  #_(:require [clojure.data.xml :refer [emit-str]]
            #_[glider.domains.legacy.wrapper.xml :refer [parse-xml]]
            [glider.domains.legacy.transaction.utils
             :refer [http-post-request
                     send-request!
                     process-request
                     paginate-xml
                     parse-xml-file
                     page-stream]]))

#_(defn println-to-str [m]
  (with-out-str (clojure.pprint/pprint m)))

(defn all-projects-transaction []
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
         :content [{:tag :projectStatusCde, :content ["pub"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["Project_DS"]}
          {:tag :operationType, :content ["fetch"]}
          {:tag :textMatchStyle, :content ["exact"]}]}
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
        #_ {:tag :sortBy,
         :attrs {:xsi:type "xsd:List"},
         :content [{:tag :elem, :content ["projectId"]}]}
        {:tag :componentId, :content ["isc_ManageProjectModule$2_0"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["mainProjectSearch"]}]}]}]})


(defn project-transaction [project-id]
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
         :content [{:tag :projectId, :content [project-id]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["ProjectEdit_DS"]}
          {:tag :operationType, :content ["fetch"]}]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["viewAllProjectSearch"]}]}]}]})

(defn project-surveys-transaction [project-id]
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
         :content
         [{:tag :projectId, :content [project-id]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["Survey_DS"]}
          {:tag :operationType, :content ["fetch"]}
          {:tag :textMatchStyle, :content ["exact"]}]}
        #_ {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        #_ {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["10"]}
        #_ {:tag :componentId, :content ["isc_SearchSurveyWindow$2_1"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["viewSurveySheetMain"]}]}]}]})

#_(defn get-project-surveys [project-id cookie]
  (-> (emit-str (project-surveys-transaction project-id))
       (http-post-request cookie)
       process-request))

#_(defn get-project [project-id cookie]
  (-> (emit-str (project-transaction project-id))
       (http-post-request cookie)
       process-request))

#_(defn get-projects [cookie]
  (-> (emit-str (all-projects-transaction))
      (http-post-request cookie)
      send-request!))
