(ns glider.event-store.core
  (:require [java-time :refer [local-date local-date-time as] :as jt]
            [malli.core :as m]
            [malli.util :as mu]
            [malli.error :as me]
            [malli.generator :as mg]
            [malli.transform :as mt]))

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

(generate-event)

(def store (atom []))

(defn add-event [event]
  (swap! store #(conj % event)))

(defn read-events []
  @store)

(add-event {:id 2})

(take 3 (read-events))

(defmulti command-handler :command)

(defmethod
  command-handler
  :create-taxon-observation [{:keys [observation]}]
  ;create event metadata
  {:type :taxon-was-observed
   :uuid ...
   :creation-date ...
   :payload observation}
  ;merge in payloads
  (-> #p (m/encode
           taxon-was-observed-schema
           observation
           mt/json-transformer)
      add-event)
  )

(command-handler
  {:command :create-taxon-observation
   :observation (generate-event)})

; http request | cli request
; |-> request handler (check auth - parse params ...)
;       reitit - malli
; |-> command handler (read events to build state - return events)
;        multi-method - malli
; |-> hapend event (return nil)
; |-> (react to event)



