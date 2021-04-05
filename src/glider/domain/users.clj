(ns glider.domain.users
  (:require [glider.event-store.core :as db]
            [glider.legacy.transaction.users :as users]))

; persist migrated user
; userUid
(defn persist-migrated-user [user]
  (let [userUid (get user "userUid" 0)]
    (db/query-event-stream ::users ["userUid"] userUid)) 
  )
(comment 
  (persist-migrated-user nil)
  )
