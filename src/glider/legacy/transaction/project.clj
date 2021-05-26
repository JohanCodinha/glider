(ns glider.legacy.transaction.project
  (:require [clojure.data.xml :refer [emit-str]]
            [glider.legacy.transaction.xml :refer [parse-xml]]
            [glider.legacy.utils
             :refer [http-post-request
                     send-request!
                     paginate-xml
                     parse-xml-file
                     page-stream]]))

#_(defn println-to-str [m]
  (with-out-str (clojure.pprint/pprint m)))
(comment
  (def project-detail (parse-xml "personnel-project.xml")))

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
       [{:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["Project_DS"]}
          {:tag :operationType, :content ["fetch"]}
          {:tag :textMatchStyle, :content ["exact"]}]}
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
        {:tag :componentId, :content ["isc_ManageProjectModule$2_0"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["mainProjectSearch"]}]}]}]})
(comment
  (parse-xml "survey-project.xml")
  {:tag :transaction,
   :attrs
   {:xsi:type "xsd:Object",
    :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"},
   :content
   [{:tag :transactionNum,
     :attrs {:xsi:type "xsd:long"},
     :content ["9"]}
    {:tag :operations,
     :attrs {:xsi:type "xsd:List"},
     :content
     [{:tag :elem,
       :attrs {:xsi:type "xsd:Object"},
       :content
       [{:tag :criteria,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :projectId, :attrs nil, :content ["3707"]}
          {:tag :isReportMode,
           :attrs {:xsi:type "xsd:boolean"},
           :content ["true"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :attrs nil, :content ["Survey_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}
          {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow,
         :attrs {:xsi:type "xsd:long"},
         :content ["1000"]}
        {:tag :componentId,
         :attrs nil,
         :content ["isc_SearchSurveyWindow$2_0"]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["viewSurveySheetMain"]}]}]}]})



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
         :content
         [{:tag :projectId, :attrs nil, :content [project-id]}
          {:tag :isReportMode,
           :attrs {:xsi:type "xsd:boolean"},
           :content ["true"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :attrs nil, :content ["Survey_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}
          {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow,
         :attrs {:xsi:type "xsd:long"},
         :content ["1000"]}
        {:tag :componentId,
         :attrs nil,
         :content ["isc_SearchSurveyWindow$2_0"]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["viewSurveySheetMain"]}]}
      {:tag :elem,
       :attrs {:xsi:type "xsd:Object"},
       :content
       [{:tag :criteria,
         :attrs {:xsi:type "xsd:Object"},
         :content [{:tag :PROJECT_ID, :attrs nil, :content [project-id]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :attrs nil, :content ["PermitType_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation, :attrs nil, :content ["fetchPermits"]}]}
      {:tag :elem,
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
        {:tag :operation, :content ["viewAllProjectSearch"]}]}
      {:tag :elem,
       :attrs {:xsi:type "xsd:Object"},
       :content
       [{:tag :criteria,
         :attrs {:xsi:type "xsd:Object"},
         :content [{:tag :projectId, :attrs nil, :content ["3707"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :attrs nil, :content ["ProjectEdit_DS"]}
          {:tag :operationType, :attrs nil, :content ["fetch"]}]}
        {:tag :appID, :attrs nil, :content ["builtinApplication"]}
        {:tag :operation,
         :attrs nil,
         :content ["fetchPersonnelForProject"]}
        ]}
      {:tag :elem,
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
        #_{:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        #_{:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["10"]}
        #_{:tag :componentId, :content ["isc_SearchSurveyWindow$2_1"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["viewSurveySheetMain"]}]}]}]})


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
