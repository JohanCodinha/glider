(ns glider.wrapper.survey
  (:require [clj-http.client :refer [request] :as http]
            [clojure.data.xml :refer [emit-str]]
            [glider.wrapper.js-parser :refer [parse-js-object]]
            [glider.wrapper.utils
             :refer [http-post-request
                     send-request
                     paginate-xml
                     parse-xml-file
                     page-stream]]))


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
          {:tag :isGeneralObservation,
           :attrs {:xsi:type "xsd:boolean"},
           :content ["true"]}]}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["Survey_DS"]}
          {:tag :operationType, :content ["fetch"]}]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["fetchSurvey"]}]}]}]})

(defn get-survey [surveyId cookie]
  (-> (http-post-request (emit-str (survey-transaction surveyId)) cookie)
      send-request
      first
      :data))


