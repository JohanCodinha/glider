(ns glider.wrapper.project
  (:require [clojure.data.xml :refer [emit-str]]
            [glider.wrapper.xml :refer [parse-xml]]
            [glider.wrapper.utils
             :refer [http-post-request
                     process-request
                     paginate-xml
                     parse-xml-file
                     page-stream]]))

(defn println-to-str [m]
  (with-out-str (clojure.pprint/pprint m)))

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
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["1000"]}
        {:tag :componentId, :content ["isc_SearchSurveyWindow$2_1"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["viewSurveySheetMain"]}]}]}]})

(defn get-project-surveys [project-id cookie]
  (-> (emit-str (project-surveys-transaction project-id))
       (http-post-request cookie)
       process-request))

(defn get-project [project-id cookie]
  (-> (emit-str (project-transaction project-id))
       (http-post-request cookie)
       process-request))
