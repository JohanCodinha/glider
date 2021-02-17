(ns glider.domains.users
  (:require
    [glider.domains.legacy.wrapper.users :as users]
    [glider.event-store.core :as db]))


; persist migrated user
; userUid
(defn persist-migrated-user [user]
  (let [userUid (get user "userUid" 0)]
    (db/query-event-stream ::users ["userUid"] userUid)) 
  )
(comment 
  (persist-migrated-user nil)
  )
