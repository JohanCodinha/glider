(ns glider.wrapper.utils
  (:require [clojure.zip :as zip]
            [diehard.core :as dh]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.data.xml :refer [emit-str]]
            [glider.wrapper.js-parser :refer [parse-js-object]]
            [clojure.data.zip.xml :as zx]
            [glider.wrapper.xml :refer [parse-xml]]
            [glider.wrapper.lookup :refer [resolve-key lookup-transaction]]
            [clj-http.client :refer [request] :as http]))

defn run-myfunc []
  (let [start-time (System/nanoTime)]
    (myfunc)
    (/ (- (System/nanoTime) start-time) 1e9))

(defmacro time-value
  "Evaluates expr and return tuple with the time it took and return value."
  {:added "1.0"}
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
      [(/ (double (- (. System (nanoTime)) start#)) 1000000.0) ret#]))

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

(defn page-stream [step]
  (iterate
   (fn [[start end]]
     [(inc end) (inc (+ end (- end start)))])
   [0 step]))

(defn login-required? [s]
  (boolean (re-find #"isc_loginRequired" s)))

(defn paginate-request [transaction cookie start-row end-row]
  (-> (paginate-xml transaction start-row end-row)
      emit-str
      (http-post-request cookie)))

(defn send-request [options]
  (dh/with-retry {:retry-on          [
                                      java.net.UnknownHostException
                                      ]
;                  :abort-on [java.net.UnknownHostException]
                  :max-retries       2
                  :on-retry          (fn [val ex] (prn ex "retrying..."))
                  ; :retry-if          (fn [val ex] (println "retry-if: ") (prn ex))
                  :on-failure        (fn [_ _] (prn "failed..."))
                  :on-failed-attempt (fn [_ _] (prn "failed attempt"))
                  :on-success        (fn [_] (prn "did it! success!"))}
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
                                 :response response}))))
            (throw-when-json-array-not-conform [parsed-json]
              (when (not= 1 (count parsed-json))
                (throw (ex-info "Host response array doesnt contain a single value"
                                {:type ::invalid-response
                                 :request options
                                 :response parsed-json}))))]
      (-> (request options)
        (doto throw-when-login-required)
        (doto throw-when-host-error)
        :body
        (parse-js-object)
        (doto throw-when-json-array-not-conform)
        first
        (doto throw-when-invalid-response)))))

(defn fetch-rows [transaction cookie]
  "Fetch all rows for a given xml rpc payload"
  (->>
    (reduce
      (fn [acc [start-row end-row]]
        (let [req (paginate-request
                        transaction
                        cookie
                        start-row
                        end-row)
              [res-time res] (time-value (send-request req))
              acc-data (conj acc res)]
          (println
            (str "fetched " (-> (get res "data") count) " rows in " (format "%.2f" (/ res-time 1000)) "s"))
          
          (println
            (str "percentage done : "
                 (format "%.2f"
                   (* 100
                      (double
                        (/ (count (mapcat #(get % "data") acc-data))
                           (get res "totalRows")))))))
          (println (str "total rows: " (-> (get res "totalRows"))))
          (println (str "row left : "
                        (- (-> (get res "totalRows")) (count acc-data))))
          (if (>= (get res "totalRows") end-row)
            acc-data
            (reduced acc-data))))
      [] (page-stream 150))
    (mapcat #(get % "data"))))

(def lookup-table
  (read-string (slurp "resources/lookup-table.edn")))

(defn process-request [options]
  (-> (send-request options)
      (get "data")))

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

(defn get-lookups [cookie]
  (-> (http-post-request (emit-str lookup-transaction) cookie)
      process-request))

(def get-lookups-memo (memoize get-lookups))

(defn lookups [cookie]
  (->> (get-lookups-memo cookie)
       (group-by :lookup-type-txt)
       (map (fn [[k v]] [(->kebab-case-keyword k) v]))
       (into {})))
