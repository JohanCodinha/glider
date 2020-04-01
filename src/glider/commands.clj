(ns glider.commands
  (:require [malli.util :as mu]
           [malli.core :as m]))
(defn uuid [] (java.util.UUID/randomUUID))

(defn timestamp [] (quot (System/currentTimeMillis) 1000))

(def command-schema
  [:map
   [:id uuid?]
   [:name keyword?]
   [:timestamp int?] 
   [:data map?]])

(defrecord command [id name timestamp data])

(defn make-command [name data]
  {:post [(m/validate command-schema %)]}
  (->command (uuid) name (timestamp) data))

(defn publish-observation [data] (make-command ::publish-observation data))

:glider.commands/publish-observation
(comment
  (make-command :publish-observation {:name :possum})
  (m/explain command-schema
             {:id (uuid)
              :timestamp 12345
              :data {}})

  (m/validate [:map [:query [:map [:cmd string?] [:data map?]]]]
              {:query {:cmd "test"
                       :data {}}}) )


