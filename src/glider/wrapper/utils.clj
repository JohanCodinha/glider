(ns glider.wrapper.utils
  (:require [clojure.zip :as zip]
            [glider.wrapper.js-parser :refer [parse-js-object]]
            [clojure.data.zip.xml :as zx]
            [clj-http.client :refer [request] :as http]))

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

(defn send-request [options]
  (letfn [(throw-when-login-required [response]
            (when (login-required? response)
              (throw (ex-info "login is required"
                         {:type ::login-required
                          :response response}))))
          (throw-when-invalid-response [response]
            (when (= -1 (:status response))
              (throw (ex-info "Invalid response response"
                              {:type ::invalid-response
                               :request options
                               :response response}))))]
    (-> (request options)
        :body
        (doto throw-when-login-required)
        (parse-js-object)
        (doto throw-when-invalid-response)
        :data)))
