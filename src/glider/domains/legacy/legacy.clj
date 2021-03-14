(ns glider.domains.legacy.legacy
  (:require [glider.domains.legacy.transaction.users :as users]
            [glider.domains.legacy.transaction.utils :as utils]
            [glider.event-store.core :as es]))

#_(defn vba-user->user-retrieved-event
    "map an imported vba user to a saved event"
    [raw-json-user]
    (es/payload->events {:type ::retrieved-user
                         :payload raw-json-user}))

#_(defn fetch-user->save-as-event!
  [cookie]
  (transduce 
    (comp 
      (map #(do (utils/fetched-rows-report %)
                %))
      (mapcat #(get % "data"))
      (map vba-user->user-retrieved-event)
      )
    conj []
    (users/get-all-users! cookie)))

#_(comment
  (doseq [event fetched-user] (es/append-to-stream ::user 1 event)))


;fetch users
;if users exist in db
  ;persist change
  ;persist users

