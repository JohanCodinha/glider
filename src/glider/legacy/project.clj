(ns glider.legacy.project
  (:require [glider.legacy.auth :as legacy-auth]
            [glider.legacy.transaction.project :as transactions]
            [glider.legacy.utils :as utils]
            [glider.system.command.core :as command]
            [glider.utils :refer [uuid timestamp]]
            [editscript.core :as diff]
            [glider.db :refer [select insert! execute!]]))

(defn get-all-projects!
  "Fetch all projects, return a lazy seq"
  [cookie]
  (utils/request! (transactions/all-projects-transaction) #_ 100 cookie))

(comment
  (def projectId "3707")
  (def projects (get-all-projects! @legacy-auth/admin-cookie))

  (take 2 (get (first projects) :data))
  (dissoc (first projects) :data))
(comment
  (def c (utils/request2! (transactions/project-transaction "3707") @legacy-auth/admin-cookie))
  (def g (utils/request2! (transactions/project-transaction "1") @legacy-auth/admin-cookie))
  (def d (utils/request! (transactions/project-transaction "2") @legacy-auth/admin-cookie)))

(comment
  (legacy-auth/refresh-cookie)

  (def g2 (vec g))
  
  )

(def legacy-cookie
  [:cookie
   (fn [ctx]
     (let [previous-request (count (filter #{:cookie} (:execute/stack ctx)))]
       (case previous-request
         0 @legacy-auth/admin-cookie
         1 (legacy-auth/refresh-cookie)
         (throw (ex-info "Authentication loop" ctx)))))])

(defn fetch-imported-project-stream [projectId]
  nil)

(defn fetch-project! [projectId cookie]
  (utils/request2! (transactions/project-transaction projectId) cookie))


(def current-project-state
  [:imported-project
   (fn [{{cookie :cookie} :cofx
         {:keys [projectId]} :params :as ctx}]
     (if-let [user
              (try
                (fetch-project! projectId cookie)
                (catch clojure.lang.ExceptionInfo e
                  (if (= :cookie-expired (:type (ex-data e)))
                    nil
                    (throw e))))]
       user
       (command/enqueue ctx legacy-cookie)))])

(defn legacy-project-synchronized-event
  [data projectId version]
  {:id (uuid)
   :type :legacy-user-synchronized
   :stream-id projectId
   :created-at (timestamp)
   :version version
   :payload data})

(defn diff-payload [saved imported]
  (let [saved-by-operation (into {} (map (juxt :operation identity) saved))]
  (if saved
    (->> imported
         (map (fn [{operation :operation :as i}]
                (update i :data #(diff/get-edits
                                  (diff/diff (:data (get saved-by-operation
                                                         operation))
                                             %)))))
         (remove #(empty? (:data %)))
         vec)
    imported)))

;;Import users from legacy app

(defn merge-diffs
  "Merge a stream of diff, return nil if passed empty vector"
  [stream]
  (reduce
   (fn
     ([])
     ([a b]
      (mapv (fn [{operation :operation :as payload}]
             (let [data-b (:data (first (get (group-by :operation b) operation)))]
               (cond-> payload
                 (and (some? data-b) (diff/valid-edits? data-b))
                 (update :data #(diff/patch % (diff/edits->script data-b)))
                )))
           a)))
   (map :payload stream)))

(def import-project-command!
  {:id ::import-project
   :params [:map
            [:projectId
             [:string]]]
   :coeffects [legacy-cookie
               [:imported-project-stream
                (fn [{{:keys [projectId]} :params}]
                  (fetch-imported-project-stream projectId))]
               current-project-state]
   :effects (fn [{{:keys [imported-project imported-project-stream]} :cofx
                  {:keys [projectId]} :params}]
              (let [saved-project (merge-diffs imported-project-stream)
                    next-version (-> imported-project-stream
                                     last
                                     (get :version 0)
                                     inc)

                    new-data (diff-payload saved-project
                                           (map
                                            #(select-keys % [:data :operation])
                                            imported-project))]
                (merge nil
                       (when-not (empty? new-data)
                         {:save-legacy-event (legacy-project-synchronized-event new-data projectId next-version)})
                       )))
   :handler {:save-legacy-event #(insert! :legacy-events %)}
   :return identity
   :produce [:legacy-project-synchronized]})


(comment
  (command/run! import-project-command!
                {:projectId "3707"}
                {:side-effects false})
  )
