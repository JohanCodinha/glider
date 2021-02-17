(ns glider.domains.legacy.wrapper.survey
  (:require [clj-http.client :refer [request] :as http]
            [clojure.data.xml :refer [emit-str]]
            [glider.domains.legacy.wrapper.js-parser :refer [parse-json]]
            [glider.domains.legacy.wrapper.utils
             :refer [http-post-request
                     process-request
                     paginate-xml
                     parse-xml-file
                     page-stream]]))

(defn observation-attachements-transaction [record-id]
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
       [{:tag :values,
         :attrs {:xsi:type "xsd:Object"},
         :content [{:tag :pk, :content [(str "taxonRecorded_" record-id)]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["Taxon_DS"]}
          {:tag :operationType, :content ["custom"]}]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["listFiles"]}]}]}]})

(defn aquatic-survey-component-transaction [component-id]
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
         [{:tag :SURVEY_COMPONENT_ID,
           :attrs {:xsi:type "xsd:long"},
           :content [component-id]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["AquaticSurveyComponent_DS"]}
          {:tag :operationType, :content ["fetch"]}]}
        {:tag :componentId, :content ["isc_DynamicForm_50"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["AquaticSurveyComponent_DS_fetch"]}]}]}]})

(defn taxon-summary-transaction [component-id]
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
         [{:tag :componentId, :content [component-id]}
          {:tag :_forceToRefresh, :content ["1574908208523"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["SurveyCompSummaryView_DS"]}
          {:tag :operationType, :content ["fetch"]}
          {:tag :textMatchStyle, :content ["exact"]}]}
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["5"]}
        {:tag :componentId,
         :content ["isc_TaxonRecordedSummaryTab$23_1"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["fetchDataForSC"]}]}]}]})

(defn component-transaction [component-id]
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
         :content [{:tag :SURVEY_COMPONENT_ID, :content [component-id]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["SurveyComponent_DS"]}
          {:tag :operationType, :content ["fetch"]}]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["SurveyComponent_DS_fetch"]}]}]}]})

(defn methods-transaction [survey-id]
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
         :content [{:tag :surveyId, :content [survey-id]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["SurveyComponent_DS"]}
          {:tag :operationType, :content ["fetch"]}
          {:tag :textMatchStyle, :content ["exact"]}]}
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
        {:tag :componentId,
         :content ["isc_TaxonRecordedSummaryTab$20_0"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["fetchSurveyComponentBySurveyID"]}]}]}]})

(defn site-detail-transaction [site-id]
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
         [{:tag :siteId, :content [site-id]}
          {:tag :isAForAI,
           :attrs {:xsi:type "xsd:boolean"},
           :content ["true"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["SiteDetail_DS"]}
          {:tag :operationType, :content ["fetch"]}]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["fetchSiteDetail"]}]}]}]})


(defn survey-transaction [surveyId]
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
         [{:tag :surveyId, :content [surveyId]}
          #_ {:tag :isGeneralObservation,
           :attrs {:xsi:type "xsd:boolean"},
           :content ["true"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["Survey_DS"]}
          {:tag :operationType, :content ["fetch"]}]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["fetchSurvey"]}]}]}]})

(defn get-survey [survey-id cookie]
  (-> (survey-transaction survey-id)
      emit-str
      (http-post-request cookie)
      process-request))

(defn get-site-details [site-id cookie]
  (-> (site-detail-transaction site-id)
      emit-str
      (http-post-request cookie)
      process-request))

(defn get-methods [survey-id cookie]
  (-> (methods-transaction survey-id)
      emit-str
      (http-post-request cookie)
      process-request))

(defn get-taxon-summary [survey-id cookie]
  (-> (taxon-summary-transaction survey-id)
      emit-str
      (http-post-request cookie)
      process-request))

(defn get-aquatic-survey-component [component-id cookie]
  (-> (aquatic-survey-component-transaction component-id)
      emit-str
      (http-post-request cookie)
      process-request))

(defn get-attachements [record-id cookie]
  (-> (observation-attachements-transaction record-id)
      emit-str
      (http-post-request cookie)
      process-request
      parse-json))
