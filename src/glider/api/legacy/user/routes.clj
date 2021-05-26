(ns glider.api.legacy.user.routes
  (:require [glider.api.legacy.user.handler :as user]
            [glider.legacy.users :refer [import-user-command]]))
(defn routes
  [env]
  (let [db (:db env)]
    ["/legacy/synchronization/user"
     {:post {:summary (str "One way synchronization of users from https://vba.dse.vic.gov.au
                            Produced events: ")
             :tags ["Command" "Legacy" "User"]
             :responses {200 {:description "Synchronized a single user"
                              :body [:map [:command map?]]}}
             :parameters {:body (:params import-user-command)}
             :handler (user/fetch-by-userUid db)}}]))
