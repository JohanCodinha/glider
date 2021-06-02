(ns glider.api.legacy.user.handler
  (:require 
            [glider.legacy.users :refer [import-user-command import-users-command]]
            [glider.system.command.core :as command]
            [glider.system.operation.core :as operation]))

(defn import-by-userUid [db]
  (fn [req]
    (println "Vba sync requested for:"
             (-> req :parameters :path :userUid))
    (let [command-return
          (command/run! import-user-command
                        (-> req :parameters :path)
                        {:side-effects true
                         :environment {:db db}})]
      {:status 200
       :body command-return})))

(defn import-all [db]
  (fn [req]
    (let [operation (operation/create-operation db {:metadata {:name :import-all-users}})]
      (tap> (command/run! import-users-command
                          {}
                          {:side-effects false
                           :operation operation
                           :environment {:db db}}))
      {:status 202
       :body (assoc operation :status-url (str "/operation/" (:id operation)))})))

(comment
  ((import-all @glider.db/datasource) {})

  (command/run! import-users-command
                {}
                {:side-effects false
                 :environment {:db @glider.db/datasource
                               :operation (operation/create-operation @glider.db/datasource
                                                                      {:metadata {:name :import-all-users}})}}))
