(ns glider.system.operation.db
  (:require [next.jdbc :as jdbc]
            [glider.db :as db :refer [insert! select!]]
            [glider.utils :as utils]))

(def DEFAULT-JOB-TABLE "job")

(defn enqueue!
  [db job]
  (insert! db DEFAULT-JOB-TABLE job))

(defn get-by-uuid!
  [db uuid]
  (select! db [(format "SELECT * FROM %s WHERE id = ?" DEFAULT-JOB-TABLE) uuid]))

(defn get-all-running!
  [db]
  (select! db [(format "SELECT * FROM %s WHERE done = false" DEFAULT-JOB-TABLE)]))

(comment
  (jdbc/execute!
   @db/datasource
   [(format "CREATE TABLE %s (id uuid NOT NULL, enqueued_at timestamp, done BOOLEAN NOT NULL, result jsonb, metadata jsonb NOT NULL)"
            DEFAULT-JOB-TABLE)])
  
  (jdbc/execute! @db/datasource
                 [(format "DROP TABLE %s "
                          DEFAULT-JOB-TABLE)])
  
  (get-by-uuid! @db/datasource (java.util.UUID/fromString "13398eb7-7d9a-4d4e-8b98-ecc67813eeea"))

  (enqueue! @db/datasource {:id (utils/uuid "13398eb7-7d9a-4d4e-8b98-ecc67813eeea")
                           :enqueued_at (utils/timestamp)
                           :done false
                           :metadata {:state :processing
                                      :total-user 9999
                                      :fetched 10}}))
