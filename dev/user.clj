(ns user
  (:require [camel-snake-kebab.core :as csk]
            [clj-fuzzy.metrics :as fuzzy]
            [clj-http.client :as http :refer [request]]
            [clojure.data.xml :refer [emit-str]]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [editscript.core :as c]
            [editscript.edit :as e]
            [glider.legacy.transaction.project :as project]
            [glider.event-store.core :as es]
            [glider.legacy.transaction.general-obs :as general-obs]
            [glider.legacy.transaction.login :as login]
            [glider.legacy.transaction.lookup :as lookup]
            [glider.legacy.transaction.site :as site]
            [glider.legacy.transaction.survey :as survey]
            [glider.legacy.transaction.users :as legacy-users]
            [glider.legacy.users :as users]
            [glider.legacy.utils
             :as
             utils
             :refer
             [http-post-request
              page-stream
              paginate-xml
              parse-xml-file
              send-request!]]
            [glider.system :as system]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl])
  (:import com.impossibl.postgres.api.jdbc.PGNotificationListener
           com.impossibl.postgres.jdbc.PGDataSource))

(comment
  (def datasource (doto (PGDataSource.)
                    (.setHost     "localhost") ; todo move into
                    (.setPort     5432)
                    (.setDatabaseName "glider")
                    (.setUser     "sugar")
                    (.setPassword "surfing")))


; create a listener that triggers when a message is received


  (def listener
    (reify PGNotificationListener
      (^void notification [this ^int processId ^String channelName ^String payload]
        (println "msg: " payload))))


; setup a connection with the listener


  (def connection
    (doto (.getConnection datasource)
      (.addNotificationListener listener)))


; begin listening to a channel


  (doto (.createStatement connection)
    (.execute "LISTEN mymessages;")
    (.close)))

(ig-repl/set-prep! (fn [] system/system-config))

(ig/load-namespaces system/system-config)

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)


(comment
  (go)
  (halt)
  (reset)
  (reset-all))

(comment
  (def admin_username (System/getenv "admin_username"))
  (def admin_password (System/getenv "admin_password"))
  (def mel_username (System/getenv "mel_username"))
  (def mel_password (System/getenv "mel_password"))

  (def cookie ((memoize login/login->cookie)
               admin_username admin_password))

  (def mel_cookie ((memoize login/login->cookie)
                   mel_username mel_password))

  {:vlaaad.reveal/command '(clear-output)}

  (def fetched-user (legacy/fetch-user->save-as-event! cookie))
 ;; pass custom error to request function

  (def new-users (users/get-all-users! cookie) #_(mapcat #(get % "data")))
  (doall new-users)
  (count new-users)

  (def old-user (clojure.edn/read-string (slurp "users.edn")))
  (count old-user)
  (clojure.data/diff old-user new-users)

  (utils/fetch-rows! legacy-users/all-users-transaction 100 cookie)

  (first (utils/fetch-rows!
          {:tag :transaction
           :attrs
           {:xsi:type "xsd:Object"
            :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"}
           :content
           [{:tag :operations
             :attrs {:xsi:type "xsd:List"}
             :content [{:tag :elem
                        :attrs {:xsi:type "xsd:Object"}
                        :content [{:tag :criteria
                                   :attrs {:xsi:type "xsd:Object"}
                                   :content []}
                                  {:tag :operationConfig
                                   :attrs {:xsi:type "xsd:Object"}
                                   :content [{:tag :dataSource
                                              :content ["UserInfoShowView_DS"]}
                                             {:tag :operationType
                                              :content ["fetch"]}
                                             #_{:tag :textMatchStyle
                                                :content ["exact"]}]}
                                  {:tag :componentId
                                   :content ["isc_ManageUserAdminModule$2_0"]}
                                  {:tag :appID
                                   :content ["builtinApplication"]}
                                  {:tag :operation
                                   :content ["userInfoMainSearch"]}
                                  {:tag :oldValues
                                   :attrs {:xsi:type "xsd:Object"}
                                   :content []}]}]}]} 10 cookie))

  (utils/xml-datasources (legacy-users/user-details 10))
  (utils/xml-datasources (legacy-users/all-users-transaction))
  ;; => ("UserInfoShowView_DS")
  ;; => ("UserInfoView_DS"
;;     "UserOrganisationLink_DS"
;;     "AddressDetail_DS"
;;     "ContactDetail_DS")
  (utils/request-raw!
   (utils/paginate-datasource (legacy-users/user-details 757) "ContactDetail_DS" 0 2)
   cookie)
  (utils/request-raw!
   (utils/paginate-datasource (legacy-users/all-users-transaction) "UserInfoShowView_DS" 0 2)
   cookie)
  (utils/request-raw!
   (utils/paginate-datasource (legacy-users/meta-trans 757) "UserInfoShowView_DS" 0 2)
   cookie)

  (let [transaction (legacy-users/user-details 757)]
    (utils/match-transaction-response transaction
                                      (utils/request-xml! transaction cookie)))

  (let [transaction (utils/paginate-datasource (legacy-users/all-users-transaction) "UserInfoShowView_DS" 0 2)]
    (utils/match-transaction-response transaction
                                      (utils/request-xml! transaction cookie)))

  ;; => ("UserInfoShowView_DS")

  (def codeforvic-2 (-> (emit-str legacy-users/user-details)
                        (http-post-request cookie)
                        utils/process-request-multi))

  (utils/request-xml! (legacy-users/user-details 757) cookie)


  ;;(events->rows-cols fetched-user)


  (doseq [event (take 1 fetched-user)] (es/append-to-stream
                                        event))

  (es/append-to-stream-multi! (vec (take 2 fetched-user)))

  (def prev-user (mapcat #(get % "data") prev-user))

  (def prev-user-Uid (set (map #(get % "userUid") prev-user)))

  #_(def n-user (filter
                 (fn [u] (not (clojure.core/contains? prev-user-Uid (get u "userUid"))))
                 new-all-users))
  (= (utils/paginate-datasource legacy-users/all-users-transaction "nope" 0 1)
     (utils/paginate-xml legacy-users/all-users-transaction 10 100))

  (utils/request-xml!
   (utils/paginate-datasource (legacy-users/user-details 757) "ContactDetail_DS" 0 1)
   cookie)

  ;; => ("UserInfoView_DS"
  ;;     "UserOrganisationLink_DS"
  ;;     "AddressDetail_DS"
  ;;     "ContactDetail_DS")

  (try
    (-> mel_cookie
        login/get-user-details)
    (catch Exception e
      (println e)))

  (def cfaobs (general-obs/get-user-general-obs cookie))

  {:contributors "johan codinha"
   :modifiedTsp 1499912034725
   :siteNme "user location"
   :surveyId 1394300
   :surveyStartSdt "2017-05-27T14:00:00.000Z"
   :expertReviewStatusCde "del"}
  (def cfasurvey (survey/get-survey 1394300 cookie))
  (def cfb (survey/get-survey 1394300 cookie))

  (use 'clojure.data)

  (= cfasurvey cfb)

  (diff (update-in cfasurvey [:project] #(dissoc % :organisations))
        (update-in cfb [:project] #(dissoc % :organisations)))
  ;get-survey return full site information

  (with-out-str
    (pprint
     (dissoc cfasurvey :project)))

  {:primary-discipline-cde :discipline
   :date-accuracy-cde :date-accuracy
   :monitoring-protocol-cde :monitoring-protocol
   :expert-review-status-cde :expert-review}

  (spit "resources/lookup.txt" (lookup/lookups cookie))

  (spit "resources/lookup-table.edn" (pr-str lo))

  (read-string (slurp "resources/lookup-table.edn"))

  (def lo (utils/lookups mel_cookie))

  (lookup/resolve-key (dissoc cfasurvey :project) lo)

  (-> (lookup/resolve-key (dissoc cfasurvey :project) lo)
      :site
      :site-id)

  (def cookie (login/login->cookie "melvba" "vba123"))
  ;; Project
  (def project (project/get-project 3556 cookie))
  (= (project/get-project 3556 cookie) (project/get-project 3556 mel_cookie))
  (def project-surveys (project/get-project-surveys 5742 cookie))

  (spit "resources/lookup-cache.xml"
        (dissoc lookup-cache :invalidate-cache :status :is-ds-response :data))

  (def site-936419 (site/get-site 936419 cookie))

  ;; All projects
  (def all-projects (project/get-projects cookie))
  (prn all-projects)
  (def project-surveys (project/get-project-surveys 540 cookie))
  (def site-details (survey/get-site-details 1207405 cookie))

  (def sd (survey/get-site-details 411512 cookie))

  (defn get-all-keys [m]
    (flatten
     (map (fn [[k v]]
            (if (map? v)
              [k (get-all-keys v)]
              [k]))
          m)))

  (defn keyword-ending-by-cde [k]
    (re-find #"-cde$" (name k)))

  (filter keyword-ending-by-cde (get-all-keys site-details))
  (filter keyword-ending-by-cde (get-all-keys site-details))
  (:private-land-cde site-details)
  (def test-cde
    [:water-body-type-cde
     :wetland-category-cde
     :wetland-other-class-cde
     :river-basin-cde
     :surface-soil-texture-cde
     :site-shape-cde
     :open-closed-cde
     :geology-cde
     :landform-cde
     :private-land-cde
     :restricted-flag-cde])
  (def lookup-table (utils/get-lookups cookie))

  (def lookup-table
    (read-string (slurp "resources/lookup-table.edn")))

  (fuzzy/levenshtein (name :private-land-cd) "Private Land stuff")

  (defn match-cde->lookup [lookup cde]
    (take 3 (sort-by #(fuzzy/levenshtein % (name cde))
                     (->> (map :lookup-type-txt lookup-table)
                          (into #{})))))

  (into {} (map (juxt identity #(match-cde->lookup lookup-table %)) test-cde))

  (match-cde->lookup lookup-table (first test-cde)))

