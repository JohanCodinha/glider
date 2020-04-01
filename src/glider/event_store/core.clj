(ns glider.event-store.core
  (:require #_[java-time :refer
             [local-date local-date-time as] :as jt]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.error :as me]
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
   [:type keyword?]
   [:uuid uuid?]
   [:creation-date inst?]
   [:payload map?]])

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
     taxon-was-observed-schema))

(defn append-to-stream [stream-id version events]
  (db/insert! :events {:stream_id 1 :data {"bar" "foo" "list" [1 2 3]} }))

(defn read-from-stream [stream-id]
 ;return list of events 
  )


; http request | cli request
; |-> request handler (check auth - parse params ...)
;       reitit - malli
; |-> command handler (read events to build state - return events)
;        multi-method - malli
; |-> hapend event (return nil)
; |-> (react to event)



