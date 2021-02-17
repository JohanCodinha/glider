(ns glider.event-store.core
  (:require [java-time :refer
             [local-date local-date-time as] :as jt]
            [clojure.string :as st]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.error :as me]
            [malli.provider :as mp]
            [malli.generator :as mg]
            [malli.transform :as mt]
            [glider.db :as db]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :refer [insert! query delete!]]
            [cheshire.core :refer [generate-string parse-string]] ))

;add to event
;read event
(defn uuid [] (java.util.UUID/randomUUID))

(def event-schema
  [:map
   [:type [:or string? keyword?]]
   [:stream-id uuid?]
   [:created-at inst?]
   [:metadata {:optional true} [:or map? nil?]]
   [:payload map?]])

(defn payload->events
  "Generate an event"
  [{:keys [:version :metadata :payload :stream-id :type :stream-id]}]
  {:post [(m/validate event-schema %)]}
  (let [id (uuid)
        created-at (java.time.Instant/now)]
    {:id id
     :type type
     :version 1
     :stream-id (or stream-id (uuid))
     :created-at created-at
     :metadata metadata
     :payload payload}))

(comment
  (def taxon-was-observed-schema
    (mu/merge
     event-schema
     [:map
      [:payload
       [:map
        [:taxon-id int?]
        [:common-name string?]
        [:observer string?]
        [:location [:tuple double? double?]]]]]))

  (defn generate-event []
    (mg/generate
     taxon-was-observed-schema)))

(defn keyword->str [k]
  (str (.-sym k)))

(defn append-to-stream [{:keys [stream-id version events created-at metadata payload]}]
  (db/insert! :events {:stream_id stream-id
                       :created_at created-at
                       :version version
                       :metadata metadata
                       :payload payload}))

(defn dash->underscore [kword]
  (-> kword
      str
      (st/replace "-" "_")
      rest
      st/join
      keyword))

(defn margs [& args]
  (prn (count args)))

(defn append-to-stream-multi! [events]
  (apply db/insert-multi! :events
         (update-in (db/events->rows-cols events) [0] #(map dash->underscore %))))

(defn query-event-stream [stream-id path value]
  (let [json-sql-path (str "'{" (apply str (interpose "," (map name path))) "}'")
        sql (str "SELECT * FROM events WHERE stream_id = ? AND "
                     "data #> "
                     json-sql-path
                     " = ?")]
    (println json-sql-path value)
    (println sql)
    (db/select [sql
                (keyword->str stream-id)
                value]))
 ;return list of events 
  )

(defn fetch-event-stream [stream-id]
  (let [sql "SELECT * FROM events WHERE stream_id = ?"]
    (db/select [sql (java.util.UUID/fromString stream-id)])))

(comment 
  (fetch-event-stream "d6382102-9106-4872-a39c-f747d8b76225")
;; => [#:events{:id #uuid "448846bd-7a07-42cf-9ab3-c3130ce8af4f",
;;              :stream_id #uuid "d6382102-9106-4872-a39c-f747d8b76225",
;;              :type ":glider.domains.legacy/retrieved-user",
;;              :version 1,
;;              :created_at #inst "2020-09-10T05:29:49.909000000-00:00",
;;              :payload
;;              {"statusCde" "active",
;;               "organisationNmes" "Ogyris Pty Ltd",
;;               "modifiedTsp" 1592794769601,
;;               "fullName" "Ian Sluiter",
;;               "statusDesc" "Active",
;;               "roleDesc" "Contributor",
;;               "roleCde" "con",
;;               "loginNameNme" "ISluiter",
;;               "surnameNme" "Sluiter",
;;               "givenNme" "Ian",
;;               "userUid" 1},
  ;;              :metadata nil}]
  )


(comment
  (append-to-stream ::test 1 {:nope 1 :nes {:ted :ok}})

  (query-event-stream "d6382102-9106-4872-a39c-f747d8b76225" #_#_["nope"] "ok")
  (query-event-stream ))
; http request | cli request
; |-> request handler (check auth - parse params ...)
;       reitit - malli
; |-> command handler (read events to build state - return events)
;        multi-method - malli
; |-> hapend event (return nil)
; |-> (react to event)


(comment
  (mp/provide [{"test" true
                "stpo" 123}]))

