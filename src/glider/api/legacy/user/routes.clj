(ns glider.api.legacy.user.routes
  (:require [glider.api.legacy.user.handler :as user]
            [glider.legacy.users :refer [import-user-command]]))
(defn routes
  [env]
  (let [db (:db env)]
    ["/legacy/synchronization/users/:userUid"
     {:post {:summary (str "One way synchronization of a user from https://vba.dse.vic.gov.au")
             :tags ["Command" "Legacy" "Users"]
             :responses {200 {:description "Synchronized a single user"
                              :body (:produce import-user-command)}}
             :parameters {:path (:params import-user-command)}
             :handler (user/import-by-userUid db)}}]
    
    ["/legacy/synchronization/users"
     {:post {:summary (str "One way synchronization of all users from https://vba.dse.vic.gov.au")
             :tags ["Command" "Legacy" "Users"]
             :responses {202 {:description "Synchronized a single user"
                              :body [:map {:closed false}]}}
             #_#_:parameters {:path (:params import-user-command)}
             :handler (user/import-all db)}}]))
