(ns glider.api.legacy.user.handler
  (:require [glider.legacy.users :refer [import-user-command import-users-command get-all-users!]]
            [glider.legacy.auth :refer [refresh-cookie admin-cookie]]
            [glider.system.command.core :as command]
            [glider.system.operation.core :as operation]
            [editscript.edit :as e]
            [clojure.core.async :as cca]))

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

(def operation-state (atom {}))
(comment
  (::import-all-users @operation-state)
  (future-cancel (::import-all-users @operation-state)))

(defn import-all [db]
  (fn [req]
    (let [operation (operation/create-operation db {:metadata {:name :import-all-users}})
          operation-update! (partial operation/update!
                                     db
                                     (:id operation))
          task (future
                 (try
                   (let [cookie #_(refresh-cookie) @admin-cookie
                         users (get-all-users! cookie)]
                     (operation-update!
                      #(assoc-in % [:metadata :users-to-fetch] (count users)))
                     (doseq [user users]
                       (when (operation/is-paused? db (:id operation))
                         @(operation/wait-for-resume (:id operation)))
                       (let [userUid (str (get user "userUid"))
                             result (command/run! import-user-command
                                                  {:userUid userUid}
                                                  {:side-effects true
                                                   :environment {:db db}})]
                         (if-not (:error result)
                           (operation-update!
                            #(update-in % [:metadata :users-fetched] (fnil inc 0)))
                           (operation-update!
                            #(update-in % [:metadata :errors] (fnil conj []) result)))))
                     (operation-update!
                      #(assoc % :done true)))
                   (catch Exception e
                     (operation-update!
                      #(-> %
                           (assoc-in [:metadata :error] (str e))
                           (assoc :done true))))))]
      {:status 202
       :body (assoc operation :status-url (str "/operation/" (:id operation)))})))

(comment
  ((import-all @glider.db/datasource) {})

  (command/run! import-users-command
                {}
                {:side-effects false
                 :operation (operation/create-operation
                             @glider.db/datasource
                             {:metadata {:name :import-all-users}})
                 :environment {:db @glider.db/datasource}})
  ((fnil inc 0) nil))
