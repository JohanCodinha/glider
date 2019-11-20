(ns glider.wrapper.vba
  (:require [clj-http.client :refer [request] :as http]
            [clojure.data.xml :refer [emit-str]]
            [clojure.string :as str]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zip]
            [glider.wrapper.xml :refer [parse-xml]]
            [glider.wrapper.utils :refer [login-required? page-stream]]
            [glider.wrapper.login :refer
             [get-user-details login->cookie]]
            [glider.wrapper.general-obs :refer [get-user-general-obs]]))

(comment
  (def cookie ((memoize login->cookie) "username" "password"))
  (def kookie "JSESSIONID=9C7F8700F55FE2177FDF6712.worker1")
  (try 
    (-> cookie
        get-user-details)
    (catch Exception e
      (println e)))

  (def cfaobs2
    (-> cookie
        get-user-general-obs)))

(defn println-to-str [m]
  (with-out-str (clojure.pprint/pprint m)))

#_ (defn str-to-xml [str]
  (->> (parse-xml str)
       (clojure.walk/prewalk
        (fn [node]
          (if (map? node)
            (into {} (filter second node))
            node)))
       println-to-str))

(comment
  "s/\\r/\r/g"
  "s/\\" / "/g"
  (->> (parse-xml "resources/survey.xml")
       (clojure.walk/prewalk
        (fn [node]
          (if (map? node)
            (into {} (filter second node))
            node)))
       println-to-str))

(defn survey-species-transaction [id]
  (let [base-trans
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
               [{:tag :componentId, :attrs nil, :content [id]}
                {:tag :_forceToRefresh,
                 :attrs nil,
                 :content ["1565786413074"]}]}
              {:tag :operationConfig,
               :attrs {:xsi:type "xsd:Object"},
               :content
               [{:tag :dataSource,
                 :attrs nil,
                 :content ["SurveyCompSummaryView_DS"]}
                {:tag :operationType, :attrs nil, :content ["fetch"]}
                {:tag :textMatchStyle, :attrs nil, :content ["exact"]}]}
              {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
              {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
              {:tag :componentId,
               :attrs nil,
               :content ["isc_TaxonRecordedSummaryTab$23_0"]}
              {:tag :appID, :attrs nil, :content ["builtinApplication"]}
              {:tag :operation, :attrs nil, :content ["fetchDataForSC"]}]}]}]}]
    (emit-str base-trans)))

#_(defn get-species-by-survey-id [id]
  (let [cookies (login->cookie "codeforvic" "***REMOVED***")
        species (-> (survey-species-transaction id)
                    (http-post-request cookies)
                    request)]
    (-> species
        :body
        parse-js-object
        :data
        first
        (select-keys [:reliabilityCde :scientificNme :typeCde :componentId :extraCde :disciplineCde :totalCountInt :surveyId :extraDesc :loginNameNme :expertReviewStatusCde :countAccuracyCde :reliabilityDesc :id :observerId :restrictedAccessCde :samplingMethodCde :projectId :taxonId :commonNme :incidentalObsTypeCde :observerFullName]))))

(comment
  (def componentIds [1747687 1837836])
  (get-species-by-survey-id (first componentIds))
  (map get-species-by-survey-id componentIds)
  (get-user-details))

(defn mock [s e]
  (let [total 304
        take-data (inc (- (if (> e total) total e) s))]
    {:start (if (> s total) total s)
     :end (if (> e total) total e)
     :total total
     #_#_:data (if (> total (- e s))
                 (do
                   (take take-data (iterate (fn [_] (uuid)) (uuid))))
                 [])}))



(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(->> (reductions
       (fn [acc [s e]]
         (let [res (mock s e)
               _ (println "side effect")]
           (if (> (:total res) s)
             (conj acc res)
             (reduced (conj acc res))))) [] (page-stream 75))
     #_(mapcat :data)
     (take 3)
     #_(map #(dissoc % :data)))
