(ns glider.legacy.utils
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clj-http.client :as http :refer [request]]
            [clojure.data.xml :refer [emit-str]]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zip]
            
            [diehard.core :as dh]
            [glider.legacy.auth :as legacy-auth]
            [glider.legacy.transaction.js-parser :refer [parse-js-object!]]
            [glider.legacy.transaction.lookup
             :refer
             [lookup-transaction resolve-key]]
            [glider.legacy.transaction.xml :refer [parse-xml]]))


(defmacro time-request
  "Merge response map with :receiving-time-ms time-in-ms"
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (merge ret#
            {:receiving-time-ms
             (/ (double (- (. System (nanoTime)) start#)) 1000000.0)})))

(defn http-post-request [transaction cookie]
  {:method :post
   :url "https://vba.dse.vic.gov.au/vba/vba/sc/IDACall?isc_rpc=1&isc_v=SC_SNAPSHOT-2010-08-03&isc_xhr=1"
   :headers {:Cookie cookie
             :Host "vba.dse.vic.gov.au"
             :Connection "keep-alive"
             :Cache-Control "max-age=0"
             :Origin "https://vba.dse.vic.gov.au"
             :Upgrade-Insecure-Requests 1}
   :form-params {:_transaction transaction
                 :protocolVersion "1.0"}})

(defn paginate-xml [xml start end]
  "Update startRow and endRow elem from xml"
  (let [startRow {:tag :startRow
                  :attrs {:xsi:type "xsd:long"}, :content [start]}
        endRow {:tag :endRow
                :attrs {:xsi:type "xsd:long"}, :content [end]}]
    (-> (zip/xml-zip xml)
        (zx/xml1->
         :transaction
         :operations
         :elem)
        (zip/edit
         (fn [node]
           (-> node
               (update
                :content
                #(remove (fn [{tag :tag}] (#{:startRow :endRow} tag)) %))
               (update
                :content
                #(conj %
                       startRow
                       endRow)))))
        zip/root)))

(defn paginate-datasource
  "Paginate the xml query of a given datasource"
  [xml datasource start end]
  (let [startRow {:tag :startRow
                  :attrs {:xsi:type "xsd:long"}, :content [start]}
        endRow {:tag :endRow
                :attrs {:xsi:type "xsd:long"}, :content [end]}
        node (first
              (filter (zx/text= datasource)
                      (-> (zip/xml-zip xml)
                          (zx/xml->
                           :transaction
                           :operations
                           :elem
                           :operationConfig
                           :dataSource))))]
    (or (some-> node
            (zip/up)
            (zip/up)
            (zip/edit
             (fn [node]
               (update node :content
                       #(conj %
                              startRow
                              endRow))))
            zip/root)
        xml)))


(defn page-stream [step]
  (iterate
   (fn [[start end]]
     [(inc end) (inc (+ end (- end start)))])
   [0 step]))

(defn page-range
  "Given a [last-page step] return an array with range of next pages"
  ([first-row step]
   (if (= first-row 0)
     [0 (- step 1)]
     [(inc first-row) (+ first-row step)])))

(defn login-required? [s]
  (boolean (re-find #"isc_loginRequired" s)))

(defn paginate-request [transaction cookie start-row end-row]
  (-> (paginate-xml transaction start-row end-row)
      emit-str
      (http-post-request cookie)))

(defn paginate-request2 [transaction cookie start-row end-row]
  (-> (paginate-xml transaction start-row end-row)
      ))

(defn ^:dynamic *cookie-expired* [msg info]
  (throw (ex-info msg info)))

(def ^:dynamic *use-value*)

(defn send-request! [options]
  (dh/with-retry {:retry-on          [java.net.UnknownHostException java.net.SocketException]
                  :max-retries       2
                  :on-retry          (fn [val ex] (prn ex "retrying..."))
                  :on-failure        (fn [_ _] (prn "failed..."))
                  :on-failed-attempt (fn [_ _] (prn "failed attempt")
                                       (prn options))
                  #_#_:retry-if          (fn [val ex] (println "retry-if: ") (prn ex))
                  #_#_:abort-on [java.net.UnknownHostException]
                  #_#_:on-success        (fn [_] (prn "did it! success!"))}
    (let [response (request options)]
      (cond
        (login-required? (:body response))
        (binding [*use-value* identity]
          (*cookie-expired* "Legacy system requires login" {:request options
                                                            :response response
                                                            :type :cookie-expired}))

        (not= (-> (:headers response)
                  (get "Content-Type"))
              "text/plain;charset=UTF-8")
        (throw (ex-info "Host response was not plain text"
                        {:type ::host-response
                         :response response}))

        :else
        (parse-js-object! (:body response))))))
(http/get "https://vba.dse.vic.gov.au/")
(def send-request-m! (memoize send-request!))

(defn fetched-rows-report [res]
  (let [batch-fetched-rows-count (-> (get res "data") count)
        fetched-rows-count (+ 1 (get res "endRow"))
        available-rows-count (get res "totalRows")
        res-time-seconds (format "%.2f"
                                 (/ (:receiving-time-ms res) 1000))
        completion-percentage (format
                                "%.2f"
                                (* 100
                                   (double
                                     (/ (get res "endRow")
                                        (dec available-rows-count)))))]
    (println
      (str "Fetched " batch-fetched-rows-count " rows in " res-time-seconds "s"))
    (println
      (str "Completion : " completion-percentage "%"))
    (println (str "total rows: " (-> (get res "totalRows"))))
    (println (str "row left : "
                  (- available-rows-count fetched-rows-count)))))

(defn fetched-rows-report2 [{metadata :metadata :as res}]
  (let [batch-fetched-rows-count (-> (get res :data) count)
        fetched-rows-count (+ 1 (get metadata "endRow"))
        available-rows-count (get metadata "totalRows")
        res-time-seconds (format "%.2f"
                                 (/ (:receiving-time-ms res) 1000))
        completion-percentage (format
                                "%.2f"
                                (* 100
                                   (double
                                     (/ (get metadata "endRow")
                                        (dec available-rows-count)))))]
    (println
      (str "Fetched " batch-fetched-rows-count " rows in " res-time-seconds "s"))
    (println
      (str "Completion : " completion-percentage "%"))
    (println (str "total rows: " (-> (get metadata "totalRows"))))
    (println (str "row left : "
                  (- available-rows-count fetched-rows-count)))))

(defn fetch-rows-request [start-row end-row transaction cookie]
  (-> (paginate-xml transaction start-row end-row)
      emit-str
      (http-post-request cookie)))

(defn fetch-next-row!
  [prev step transaction cookie reporting-fn]
  (when (or (empty? prev)
            (> (dec (get prev "totalRows")) (get prev "endRow")))
    (lazy-seq
     (let [prev-last-row (get prev "endRow" 0)
           [start-row end-row] (page-range prev-last-row step)
           req (paginate-request transaction cookie start-row end-row)
           res (time-request (first (send-request-m! req)))
           _ (reporting-fn res)
           res-time (:receiving-time-ms res)
           prev-res-time (:receiving-time-ms prev)
           n-step (int (if (and prev-res-time (<= res-time (* 2 prev-res-time)))
                         (* 1.75 step)
                         step))]
       (cons res
             (fetch-next-row! (dissoc res "data") n-step transaction cookie reporting-fn))))))

(defn fetch-rows!
  "Lazy Fetch all rows for a given xml rpc payload, double requested row if previous request time did not double."
  [transaction init-step cookie reporting-fn]
  ((fn page-fetch [prev step]
     (when (or (empty? prev)
               (> (dec (get prev "totalRows")) (get prev "endRow")))
       (lazy-seq
        (let [prev-last-row (get prev "endRow" 0)
              [start-row end-row] (page-range prev-last-row step)
              req (paginate-request transaction cookie start-row end-row)
              res (time-request (first (send-request-m! req)))
              _ (reporting-fn res)
              res-time (:receiving-time-ms res)
              prev-res-time (:receiving-time-ms prev)
              n-step (int (if (and prev-res-time (<= res-time (* 2 prev-res-time)))
                            (* 1.75 step)
                            step))]
          (cons res (page-fetch (dissoc res "data") n-step))))))
   {} init-step))

(defn elem->transaction [elem]
  {:tag :transaction,
   :attrs
   {:xsi:type "xsd:Object",
    :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"},
   :content
   [{:tag :operations,
     :attrs {:xsi:type "xsd:List"},
     :content
     [elem]}]})

(defn xml-operations
  "Return a list of datasource from the transactions"
  [xml]
  (mapcat
   #(:content (zip/node %))
   (-> (zip/xml-zip xml)
       (zx/xml->
        :transaction
        :operations))))

(defn xml-operation
  "Return a list of datasource from the transactions"
  [xml]
  (mapcat
   #(:content (zip/node %))
   (-> (zip/xml-zip xml)
       (zx/xml->
        :transaction
        :operations
        :elem
        :operation))))


(defn xml-datasources
  "Return a list of datasource from the transactions"
  [xml]
  (mapcat
   #(:content (zip/node %))
   (-> (zip/xml-zip xml)
       (zx/xml->
        :transaction
        :operations
        :elem
        :operationConfig
        :dataSource))))


(defn hydrate-response-with-request [res xml]
  (let [operation (xml-operations xml)
        data (map #(get % "data") res)
        metadata (map #(dissoc % "data") res)]
    (mapv #(hash-map :data %1
                     :operation %2
                     :metadata %3)
          data operation metadata)))

(defn fetch-rows2!
  "Lazy Fetch all rows for a given xml rpc payload, double requested row if previous request time did not double."
  ([response cookie]
   (fetch-rows2! response cookie fetched-rows-report2))
  ([response cookie reporting-fn]
   (cons response
         ((fn page-fetch [{metadata :metadata :as prev} step]
            (when (or (empty? prev)
                      (> (dec (get metadata "totalRows")) (get metadata "endRow")))
              (lazy-seq
               (let [prev-last-row (get metadata "endRow")
                     [start-row end-row] (page-range prev-last-row step)
                     transaction
                     (paginate-request2 (elem->transaction (:operation response)) cookie start-row end-row)
                     req (http-post-request (emit-str transaction) cookie)
                     res (time-request (first (hydrate-response-with-request (send-request-m! req)
                                                                             transaction)))
                     _ (reporting-fn res)
                     res-time (:receiving-time-ms res)
                     prev-res-time (:receiving-time-ms prev)
                     n-step (int (if (and prev-res-time (<= res-time (* 2 prev-res-time)))
                                   (* 1.75 step)
                                   step))]
                 (cons res (page-fetch (dissoc res :data) n-step))))))
          (dissoc response :data) (- (get (response :metadata) "endRow")
                                     (get (response :metadata) "startRow"))))))





(defn more-rows [{total-row "totalRows"
                  start-row "startRow"
                  end-row "endRow"}]
  (and (some? total-row)
       (not= -1 end-row)
       (< end-row (dec total-row))))

(defn request! [transaction cookie]
  (let [response (-> (emit-str transaction)
                     (http-post-request cookie)
                     send-request!
                     (hydrate-response-with-request transaction))]
    response))

(defn request2! [transaction cookie]
  (let [response (-> (emit-str transaction)
                     (http-post-request cookie)
                     send-request!
                     (hydrate-response-with-request transaction))]

    (mapv (fn [res]
           (if (more-rows (:metadata res))
             (do (println "fetching more rows.")
                 (let [rows (fetch-rows2! res cookie fetched-rows-report2)]
                   (assoc (first rows) :data (vec (mapcat :data rows)))))
             res))
         response)))

(defn request-raw! [xml cookie]
  (-> (emit-str xml)
      (http-post-request cookie)
      send-request!))


(defn println-to-str [m]
  (with-out-str (clojure.pprint/pprint m)))

(defn parse-xml-file [path]
  (->> (parse-xml path)
       (clojure.walk/prewalk
         (fn [node]
           (if (map? node)
             (into {} (filter second node))
             node)))
       #_ println-to-str) )

#_(defn get-lookups [cookie]
  (-> (http-post-request (emit-str lookup-transaction) cookie)
      process-request))

#_(def get-lookups-memo (memoize get-lookups))

#_(defn lookups [cookie]
  (->> (get-lookups-memo cookie)
       (group-by :lookup-type-txt)
       (map (fn [[k v]] [(->kebab-case-keyword k) v]))
       (into {})))

(comment
  (request {:method :post, :url "https://vba.dse.vic.gov.au/vba/vba/sc/IDACall?isc_rpc=1&isc_v=SC_SNAPSHOT-2010-08-03&isc_xhr=1", :headers {:Cookie "JSESSIONID=9D12FB525351A268BA138D4E6859253A.worker1", :Host "vba.dse.vic.gov.au", :Connection "keep-alive", :Cache-Control "max-age=0", :Origin "https://vba.dse.vic.gov.au", :Upgrade-Insecure-Requests 1}, :form-params {:_transaction "<?xml version=\"1.0\" encoding=\"UTF-8\"?><transaction xsi:type=\"xsd:Object\" xmlns:xsi=\"http://www.w3.org/2000/10/XMLSchema-instance\"><operations xsi:type=\"xsd:List\"><elem xsi:type=\"xsd:Object\"><criteria xsi:type=\"xsd:Object\"><projectId>3707</projectId><isReportMode xsi:type=\"xsd:boolean\">true</isReportMode></criteria><operationConfig xsi:type=\"xsd:Object\"><dataSource>Survey_DS</dataSource><operationType>fetch</operationType><textMatchStyle>exact</textMatchStyle></operationConfig><startRow xsi:type=\"xsd:long\">0</startRow><endRow xsi:type=\"xsd:long\">1000</endRow><componentId>isc_SearchSurveyWindow$2_0</componentId><appID>builtinApplication</appID><operation>viewSurveySheetMain</operation></elem><elem xsi:type=\"xsd:Object\"><criteria xsi:type=\"xsd:Object\"><PROJECT_ID>3707</PROJECT_ID></criteria><operationConfig xsi:type=\"xsd:Object\"><dataSource>PermitType_DS</dataSource><operationType>fetch</operationType></operationConfig><appID>builtinApplication</appID><operation>fetchPermits</operation></elem><elem xsi:type=\"xsd:Object\"><criteria xsi:type=\"xsd:Object\"><projectId>3707</projectId></criteria><operationConfig xsi:type=\"xsd:Object\"><dataSource>ProjectEdit_DS</dataSource><operationType>fetch</operationType></operationConfig><appID>builtinApplication</appID><operation>viewAllProjectSearch</operation></elem><elem xsi:type=\"xsd:Object\"><criteria xsi:type=\"xsd:Object\"><projectId>3707</projectId></criteria><operationConfig xsi:type=\"xsd:Object\"><dataSource>ProjectEdit_DS</dataSource><operationType>fetch</operationType></operationConfig><appID>builtinApplication</appID><operation>fetchPersonnelForProject</operation></elem><elem xsi:type=\"xsd:Object\"><criteria xsi:type=\"xsd:Object\"><projectId>3707</projectId></criteria><operationConfig xsi:type=\"xsd:Object\"><dataSource>Survey_DS</dataSource><operationType>fetch</operationType><textMatchStyle>exact</textMatchStyle></operationConfig><appID>builtinApplication</appID><operation>viewSurveySheetMain</operation></elem></operations></transaction>", :protocolVersion "1.0"}}))
