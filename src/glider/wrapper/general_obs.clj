(ns glider.wrapper.general-obs
  (:require [clj-http.client :refer [request] :as http]
            [clojure.data.xml :refer [emit-str]]
            [glider.wrapper.js-parser :refer [parse-js-object]]
            [glider.wrapper.utils
             :refer [http-post-request paginate-xml page-stream]]))

(defn user-general-obs []
  {:tag :transaction
   :attrs
   {:xsi:type "xsd:Object"
    :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"}
   :content
   [{:tag :transactionNum
     :attrs {:xsi:type "xsd:long"}
     :content ["19"]}
    {:tag :operations
     :attrs {:xsi:type "xsd:List"}
     :content
     [{:tag :elem
       :attrs {:xsi:type "xsd:Object"}
       :content
       [{:tag :criteria
         :attrs {:xsi:type "xsd:Object"}
         :content
         [{:tag :searchMyObservations
           :attrs {:xsi:type "xsd:boolean"}
           :content ["true"]}]}
        {:tag :operationConfig
         :attrs {:xsi:type "xsd:Object"}
         :content
         [{:tag :dataSource :content ["Survey_DS"]}
          {:tag :operationType :content ["fetch"]}
          {:tag :textMatchStyle :content ["exact"]}]}
        {:tag :componentId
         :content ["isc_GeneralObservationModule$2_3"]}
        {:tag :appID :content ["builtinApplication"]}
        {:tag :operation :content ["fetchGeneralObservations"]}
        {:tag :oldValues
         :attrs {:xsi:type "xsd:Object"}
         :content
         [{:tag :searchMyObservations
           :attrs {:xsi:type "xsd:boolean"}
           :content ["true"]}]}]}]}]})

#_(defn get-user-general-obs-old [s e]
  (let [cookies (login->cookie "codeforvic" "19910908")
        endRow e
        res (->
             (paginate-xml (user-general-obs) s endRow)
             emit-str
             (http-post-request cookies)
             :body
             parse-js-object)]
    res))

(defn general-obs-request [cookie start-row end-row]
  (-> (paginate-xml (user-general-obs) start-row end-row)
      emit-str
      (http-post-request cookie)))

(defn get-user-general-obs [cookie]
  (let [pagination (page-stream 75)
        responses
        (reduce
          (fn [acc [start-row end-row]]
            (let [res (-> (general-obs-request
                            cookie
                            start-row
                            end-row)
                          request
                          :body
                          parse-js-object)
                  acc-data
                  (conj acc
                        (merge res
                               {:request-startRow start-row
                                :request-endRow end-row}))]
              (println
                (str "fetched " (-> res :data count) " record"))
              (if (>= (:totalRows res) end-row)
                acc-data
                (reduced acc-data))))
          [] pagination)]
    (mapcat :data responses)))
