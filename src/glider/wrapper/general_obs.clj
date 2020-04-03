(ns glider.wrapper.general-obs
  (:require [clojure.data.xml :refer [emit-str]]
            [glider.wrapper.utils
             :refer [http-post-request
                     send-request
                     paginate-xml
                     page-stream]]))

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

(defn general-obs-request [cookie start-row end-row]
  (-> (paginate-xml (user-general-obs) start-row end-row)
      emit-str
      (http-post-request cookie)))

(defn get-user-general-obs [cookie]
  (->>
    (reduce
      (fn [acc [start-row end-row]]
        (let [res (-> (general-obs-request
                        cookie
                        start-row
                        end-row)
                      send-request)
              _ (prn res)
              acc-data (conj acc res)]
          (println
            (str "fetched " (-> res :data count) " record"))
          (if (>= (:total-rows res) end-row)
            acc-data
            (reduced acc-data))))
      [] (page-stream 75))
    (mapcat :data)))
