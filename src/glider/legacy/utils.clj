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
           (update node :content
                   #(conj %
                          startRow
                          endRow))))
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

(defn xml-datasources-keywords [xml]
  (map #(->> %
             (re-matches #"(.*)_DS$")
             second
             keyword)
       (xml-datasources xml)))

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

(defn send-request! [options]
  (dh/with-retry {:retry-on          [java.net.UnknownHostException java.net.SocketException]
                  :max-retries       2
                  :on-retry          (fn [val ex] (prn ex "retrying..."))
                  :on-failure        (fn [_ _] (prn "failed..."))
                  :on-failed-attempt (fn [_ _] (prn "failed attempt"))
                  #_#_:retry-if          (fn [val ex] (println "retry-if: ") (prn ex))
                  #_#_:abort-on [java.net.UnknownHostException]
                  #_ #_:on-success        (fn [_] (prn "did it! success!"))}
    (letfn [(throw-when-host-error [response]
              (when (not= (-> (:headers response)
                              (get "Content-Type"))
                          "text/plain;charset=UTF-8")
                (throw (ex-info "Host response was not plain text"
                                {:type ::host-response
                                 :response response}))))
            (throw-when-login-required [response]
              (when (login-required? (:body response))
                (throw (ex-info "login is required"
                                {:type ::login-required
                                 :response response}))))
            (throw-when-invalid-response [response]
              (when (= -1 (:status response))
                (throw (ex-info "Invalid response"
                                {:type ::invalid-response
                                 :request options
                                 :response response}))))]
      (-> (request options)
        (doto throw-when-login-required)
        (doto throw-when-host-error)
        :body
        (parse-js-object!)
        (doto throw-when-invalid-response)))))

(def send-request-m! (memoize send-request!))

(defn fetched-rows-report [res]
  (println (dissoc res "data"))
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

(defn fetch-rows-request [start-row end-row transaction cookie]
  (-> (paginate-xml transaction start-row end-row)
      emit-str
      (http-post-request cookie)))

(defn fetch-rows!
  "Lazy Fetch all rows for a given xml rpc payload, double requested row if previous request time did not double."
  [transaction init-step cookie]
  ((fn page-fetch [prev step]
     (when (or (empty? prev)
               (> (dec (get prev "totalRows")) (get prev "endRow")))
       (lazy-seq
        (let [prev-last-row (get prev "endRow" 0)
              [start-row end-row] (page-range prev-last-row step)
              req (paginate-request transaction cookie start-row end-row)
              res (time-request (first (send-request-m! req)))
              res-time (:receiving-time-ms res)
              prev-res-time (:receiving-time-ms prev)
              n-step (int (if (and prev-res-time (<= res-time (* 2 prev-res-time)))
                            (* 1.75 step)
                            step))]
          (cons res (page-fetch (dissoc res "data") n-step))))))
   {} init-step))

(defn match-transaction-response [res xml]
  (let [op-names (xml-datasources xml)
        data (map #(get % "data") res)]
    (into {} (map vector op-names data))))

(defn request! [transaction]
  (-> (emit-str transaction)
      (http-post-request @legacy-auth/admin-cookie)
      send-request!
      (match-transaction-response transaction)))

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
