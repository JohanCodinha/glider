(ns glider.wrapper.lookup
  (:require [clojure.data.xml :refer [emit-str]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.walk :refer [postwalk]]
            [glider.wrapper.utils
             :refer [http-post-request
                     send-request
                     paginate-xml
                     parse-xml-file
                     page-stream]]))

(def lookup-table
  {:primary-discipline-cde :discipline
   :date-accuracy-cde :date-accuracy 
   :monitoring-protocol-cde :monitoring-protocol
   :expert-review-status-cde :expert-review})

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

(def lookup-cache-transaction
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
         [{:tag :dataSource, :content ["Lookup_DS"]}
          {:tag :operationType, :content ["fetch"]}]}
        {:tag :appID, :content ["builtinApplication"]}
        {:tag :operation, :content ["fetchLookupCache"]}
        {:tag :oldValues, :attrs {:xsi:type "xsd:Object"}}]}]}]})

(defn lookups-cache [cookie]
  (-> (http-post-request (emit-str lookup-cache-transaction) cookie)
      send-request
      first))

(defn get-lookups [cookie]
  (-> (http-post-request (emit-str lookup-transaction) cookie)
      send-request
      first
      :data))

(def get-lookups-memo (memoize get-lookups))

(defn lookups [cookie]
  (->> (get-lookups-memo cookie)
       (group-by :lookup-type-txt)
       (map (fn [[ k v]] [(->kebab-case-keyword k) v]))
       (into {})))


(defn match [k v lookup-table]
  (let [match (get lookup-table k)]
    (if match
      (some #()))))

(defn resolve-key [m lookup]
  (postwalk
    (fn [item]
      #_(println item)
      (if-let [match (and (vector? item)
                          (->> (get lookup-table (first item))
                               (get lookup)))]
        [(first item)
         (some #(when
                  (= (:lookup-cde %) (second item))
                  (:lookup-desc %))
               match)]
        item))
    m))
