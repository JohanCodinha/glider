(ns glider.system.operation.core
  (:require [glider.system.operation.db :as store]
            [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :refer [as-unqualified-maps]]
            [malli.core :as  m]
            [malli.error :as me]
            [malli.util :as mu]
            [malli.transform :as mt]
            [glider.utils :as utils]
            [clojure.core.async :as cca]))

(def operations-state (atom {}))

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

(defn update! [db operation-id f]
  (with-open [conn (jdbc/get-connection db)]
    (let [[operation] (sql/find-by-keys conn store/DEFAULT-JOB-TABLE {:id operation-id} {:builder-fn as-unqualified-maps})
          updated-operation (f operation)]
      (-> conn
          (sql/update! store/DEFAULT-JOB-TABLE updated-operation {:id operation-id} jdbc/unqualified-snake-kebab-opts)
          :next.jdbc/update-count
          (pos?)))))

(defn is-cancelled?
  [db operation-id]
  (->
   (sql/query db [(format "SELECT (metadata->':cancelled')::boolean as cancelled FROM %s WHERE id = ?" store/DEFAULT-JOB-TABLE) operation-id])
   first
   :cancelled
   boolean))

(defn is-paused?
  [db operation-id]
  (let [paused (->
                (sql/query db [(format "SELECT (metadata->':paused')::boolean as paused FROM %s WHERE id = ?" store/DEFAULT-JOB-TABLE) operation-id])
                first
                :paused
                boolean)]
    paused))

(defn wait-for-resume
  [operation-id]
  (let [resume-chan (cca/chan)]
    (swap! operations-state assoc-in [operation-id :resume] resume-chan)
    resume-chan))

(defn resume!
  [db operation-id]
  (let [resume-chan (get-in @operations-state [operation-id :resume])]
    (when resume-chan
      (update! db operation-id #(assoc-in % [:metadata :paused] false))
      (cca/>!! resume-chan :ok))))

(comment (is-cancelled? @glider.db/datasource
                        (utils/uuid  "8bdbde0e-eba4-4117-bed1-3fb97b7585b4"))
         (pause! @glider.db/datasource (utils/uuid  "eb56777e-c567-4a87-b3f7-7d92b13058ff"))
         @operations-state
         (resume! @glider.db/datasource (utils/uuid "eb56777e-c567-4a87-b3f7-7d92b13058ff"))
         (is-paused? @glider.db/datasource
                     (utils/uuid  "eb56777e-c567-4a87-b3f7-7d92b13058ff"))
         
         (find-by-id @glider.db/datasource (utils/uuid  "8bdbde0e-eba4-4117-bed1-3fb97b7585b4"))
         )
(defn cancel!
  [db operation-id]
  (update! db operation-id #(assoc-in % [:metadata :cancelled] true)))

(defn pause!
  [db operation-id]
  (update! db operation-id #(assoc-in % [:metadata :paused] true)))

(defn find-recipe-by-id
  [db recipe-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [[recipe] (sql/find-by-keys conn :recipe {:recipe_id recipe-id})
          steps (sql/find-by-keys conn :step {:recipe_id recipe-id})
          ingredeints (sql/find-by-keys conn :ingredient {:recipe_id recipe-id})]
      (when (seq recipe)
        (assoc recipe
          :recipe/steps steps
          :recipe/ingredients ingredeints)))))

(defn create-operation
  ([db] (create-operation db {}))
  ([db operation]
   (let [hydrated-operation (merge {:id (utils/uuid)
                                    :enqueued_at (utils/timestamp)
                                    :done false
                                    :metadata {}}
                                   operation)]
     (if (valid-operations? hydrated-operation)
       (do (store/enqueue! db hydrated-operation)
           )
       (throw (ex-info "Invalid operation"
                       (-> operation-schema
                           (m/explain
                            operation)
                           (me/humanize))))))))

(comment
  (last (list-operation @glider.db/datasource))

  (find-by-id @glider.db/datasource (utils/uuid  "8bdbde0e-eba4-4117-bed1-3fb97b7585b4"))

  (valid-operations?
   {:id (glider.utils/uuid "13398eb7-7d9a-4d4e-8b98-ecc67813eeea")
    :enqueued_at (glider.utils/timestamp)
    :done falsbe
    :metadata {:state :processing
               :total-user 9999
               :fetched 10}})
  (create-operation @glider.db/datasource
                    #_{:metadata {:completion 18}})

  (cancel! @glider.db/datasource
           (utils/uuid  "8bdbde0e-eba4-4117-bed1-3fb97b7585b4")
           )

  (is-cancelled? @glider.db/datasource
                 (utils/uuid  "8bdbde0e-eba4-4117-bed1-3fb97b7585b4"))

  (select-keys (last (list-operation @glider.db/datasource)) [:id]))

(def op-state (atom {:metadata {:paused false
                                :cancelled false}
                     :done false}))

(future (doseq [x (range 1000)]
          (when (-> @op-state :metadata :cancelled)
            (throw (ex-info "cancelled" {:x x})))
          (when (-> @op-state :metadata :paused)
            )
          (println x)
          (Thread/sleep 250)))

(swap! op-state update-in [:metadata :cancelled] (constantly true) )

(swap! op-state update-in [:metadata :paused] (constantly true) )

(swap! op-state update-in [:metadata :paused] (constantly false) )

