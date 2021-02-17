(ns glider.domains.legacy.users
  (:require
    [glider.domains.legacy.wrapper.users :as users]
    [glider.domains.legacy.wrapper.utils :as utils]
    [glider.event-store.core :as es]))

;; Aggregate
;; Handling users management sync with legacy system.

(defn vba-user->user-retrieved-event
  "map an imported vba user to a saved event"
  [raw-json-user]
  (es/payload->events {:type ::retrieved-user
                       :payload raw-json-user}))

(defn fetch-user->save-as-event!
  [cookie]
  (transduce 
    (comp 
      (map #(do (utils/fetched-rows-report %)
                %))
      (mapcat #(get % "data"))
      (map vba-user->user-retrieved-event))
    conj []
    (users/get-all-users! cookie)))

(defn replicate!
  [cookie]
  ;; Fetch users
  
  ;; Generate fact
  ;; Publish 
  
  )

(comment
  (doseq [event fetched-user] (es/append-to-stream ::user 1 event)))


;fetch users
;if users exist in db
  ;persist change
  ;persist users
