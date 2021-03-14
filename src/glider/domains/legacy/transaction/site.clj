(ns glider.domains.legacy.transaction.site
  #_(:require [clojure.data.xml :refer [emit-str]]
            [glider.domains.legacy.transaction.utils
             :refer
             [http-post-request page-stream paginate-xml process-request]]
            [glider.domains.legacy.transaction.xml :refer [parse-xml]]))

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

#_(defn general-obs-request [siteId cookie]
  (-> (get-site-transaction siteId)
      emit-str
      (http-post-request cookie)))

#_(defn get-site [siteId cookie]
  (-> (general-obs-request siteId cookie)
      process-request))
