(ns glider.wrapper.site
  (:require [clojure.data.xml :refer [emit-str]]
            [glider.wrapper.xml :refer [parse-xml]]
            [glider.wrapper.utils
             :refer [http-post-request
                     send-request
                     process-request
                     paginate-xml
                     page-stream]]))

(defn println-to-str [m]
(with-out-str (clojure.pprint/pprint m)))

(defn get-site-transaction [siteId]
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
       [{:tag :siteId, :content [siteId]} {:tag :LICENCE_ID}]}
      {:tag :operationConfig,
       :attrs {:xsi:type "xsd:Object"},
       :content
       [{:tag :dataSource, :content ["Site_DS"]}
        {:tag :operationType, :content ["fetch"]}]}
      {:tag :appID, :content ["builtinApplication"]}
      {:tag :operation, :content ["fetchSite"]}]}]}]})

(defn general-obs-request [siteId cookie]
  (-> (get-site-transaction siteId)
      emit-str
      (http-post-request cookie)))

(defn get-site [siteId cookie]
  (-> (general-obs-request siteId cookie)
      process-request))
