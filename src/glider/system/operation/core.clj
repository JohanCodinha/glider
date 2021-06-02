(ns glider.system.operation.core
  (:require [glider.system.operation.db :as store]
            [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [malli.core :as  m]
            [malli.error :as me]
            [malli.util :as mu]
            [malli.transform :as mt]
            [glider.utils :as utils]))

(def operation-schema
  [:map
   [:id uuid?]
   [:enqueued_at inst?]
   [:done boolean?]
   [:result {:optional true} :map]
   [:metadata :map]])

(def valid-operations?
  (m/validator operation-schema))

(defn find-by-id
  [db id]
  (first
   (sql/find-by-keys db store/DEFAULT-JOB-TABLE {:id id})))

(defn list-operation [db]
  (store/get-all-running! db))

(defn update! [db operation]
  (-> (sql/update! db store/DEFAULT-JOB-TABLE operation (select-keys operation [:id])
                   jdbc/unqualified-snake-kebab-opts)
      :next.jdbc/update-count
      (pos?)))

(defn create-operation
  ([db] (create-operation db {}))
  ([db operation]
   (let [hydrated-operation (merge {:id (utils/uuid)
                                    :enqueued_at (utils/timestamp)
                                    :done false
                                    :metadata {}}
                                   operation)]
     (if (valid-operations? hydrated-operation)
       (store/enqueue! db hydrated-operation)
       (throw (ex-info "Invalid operation"
                       (-> operation-schema
                           (m/explain
                            operation)
                           (me/humanize))))))))

(comment
  (list-operation @glider.db/datasource)
  (find-by-id @glider.db/datasource (utils/uuid "c3a4ce44-7a22-49bb-89af-0281fdb36c15"))
  (valid-operations?
   {:id (glider.utils/uuid "13398eb7-7d9a-4d4e-8b98-ecc67813eeea")
    :enqueued_at (glider.utils/timestamp)
    :done false
    :metadata {:state :processing
               :total-user 9999
               :fetched 10}})
  (create-operation @glider.db/datasource
                    #_{:metadata {:completion 18}})

  (update! @glider.db/datasource
           (update-in (last (list-operation @glider.db/datasource)) [:metadata :completion] + 1))

  (select-keys (last (list-operation @glider.db/datasource)) [:id]))

