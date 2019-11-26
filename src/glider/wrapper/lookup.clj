(ns glider.wrapper.lookup
  (:require [clojure.walk :refer [postwalk]]))

(def lookup-table
  {:primary-discipline-cde :discipline
   :date-accuracy-cde :date-accuracy 
   :monitoring-protocol-cde :monitoring-protocol
   :expert-review-status-cde :expert-review
   :project-status-cde :project-status
   :user-status-cde :project-user-status
   :user-type-cde :project-user-type
   :project-expedited-cde :expedited-project})

(def lookup-transaction
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
       [{:tag :criteria, :attrs {:xsi:type "xsd:Object"}}
        {:tag :operationConfig,
         :attrs {:xsi:type "xsd:Object"},
         :content
         [{:tag :dataSource, :content ["LookupExpansion_DS"]}
          {:tag :operationType, :content ["fetch"]}
          {:tag :textMatchStyle, :content ["substring"]}]}
        {:tag :startRow, :attrs {:xsi:type "xsd:long"}, :content ["0"]}
        {:tag :endRow, :attrs {:xsi:type "xsd:long"}, :content ["75"]}
        {:tag :componentId, :content ["isc_ReferenceDataModule$1_1"]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["LookupExpansion_DS_fetch"]}
        {:tag :oldValues, :attrs {:xsi:type "xsd:Object"}}]}]}]})

(defn trim-keyword [k regex]
  (-> k
      name
      (clojure.string/replace regex "")
      keyword))

(defn resolve-key [m lookup]
  (postwalk
    (fn [item]
      (if-let [match (and (vector? item)
                          (->> (get lookup-table (first item))
                               (get lookup)
                               (some #(when
                                        (= (:lookup-cde %) (second item))
                                        (:lookup-desc %)))))]
        [(trim-keyword (first item) #"-cde$")
         match]
        item))
    m))
