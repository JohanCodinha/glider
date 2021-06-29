(ns glider.system.command.core
  (:require [malli.core :as  m]
            [malli.error :as me]
            [malli.util :as mu]
            [malli.transform :as mt]
            [glider.system.operation.core :as operation ]
            [sieppari.core :as s]))

(defn sanitize-params [{schema :params} params]
  (if schema
    (let [sanitized (m/decode schema
                              params
                              mt/strip-extra-keys-transformer)
          valid? (m/validate schema
                             sanitized)]
      (if valid?
        sanitized
        {:system/error (me/humanize (m/explain schema sanitized))}))
    {}))

(defn execute
  "Executes a queue of Interceptors attached to the context. Context must be a map.
  Interceptor is of shape [:name fn], returned value is added to context map and passed to downstream interceptors."
  [ctx acc-key]
  (let [queue (:execute/queue ctx)
        stack (get ctx :execute/stack [])
        [interceptor-name interceptor-fn] (first queue)]
    (if (not interceptor-fn)
      (dissoc ctx :execute/queue :execute/stack)
      (recur (let [interceptor-return (interceptor-fn ctx)]
               (if (and (map? interceptor-return)
                        (contains? interceptor-return :execute/queue))
                 interceptor-return
                 (-> ctx
                     (assoc-in [acc-key interceptor-name] interceptor-return)
                     (assoc :execute/queue (rest queue))
                     (assoc :execute/stack (conj stack interceptor-name)))))
             acc-key))))

(defn enqueue [ctx interceptor]
  (update ctx :execute/queue #(vec (cons interceptor %))))


(defn condition-check [conditions {cofx :cofx params :params}]
  (when conditions
    (reduce (fn [_ [condition anomaly message]]
                   (if (condition {:cofx cofx :params params})
                     nil
                     (reduced {:anomaly anomaly
                               :message message})))
            nil
            conditions)))
(interleave [1 2 3 4] (repeat [:a :b]))

(defn update-operation
  [operation]
  [:update-operation
   (fn [{cofx :cofx
         :as ctx}]
     (operation/update!
      (get-in ctx [:environment :db])
      (assoc-in operation [:result :cofx] cofx)))])

(defn run!
  "Orchestrate the execution of a command retrieved from the command store."
  ([command]
   (run! command {} {}))
  ([command params]
   (run! command params {}))
  ([command params {run-coeffects :coeffects
                    run-handler :handler
                    run-side-effects :side-effects
                    environment :environment
                    operation :operation
                    :or {run-coeffects true run-handler true run-side-effects true}
                    :as opts}]
   (try
     (let [{params-error :system/error :as sanitized-params}
           (sanitize-params command params)]
       (if-not params-error
         (let [{coeffects :cofx} (when run-coeffects
                                   (execute {:params sanitized-params
                                             :environment environment
                                             :operation operation
                                             :execute/queue (:coeffects command)}
                                            :cofx))]
           (let [error (condition-check (:conditions command)
                                        {:cofx coeffects
                                         :params sanitized-params})]
             (if (nil? error)
               (try
                 (let [effects (when (and run-handler (:effects command))
                                 ((:effects command) {:cofx coeffects
                                                      :params sanitized-params}))
                       side-effects (when run-side-effects
                                      (let [effects (map
                                                     (fn [[effect-name effect-payload]]
                                                       [effect-name (when effect-payload
                                                                      ((get (:handler command) effect-name)
                                                                       effect-payload
                                                                       environment))])
                                                     effects)]
                                        (when-not (empty? effects)
                                          (into {} effects))))]
                   (when (:return command)
                     ((:return command) (cond-> {:params sanitized-params}
                                          run-coeffects (assoc :cofx coeffects)
                                          run-handler (assoc :handler effects)
                                          run-side-effects (assoc :side-effects side-effects)))))
                 (catch Exception e
                   (tap> command)
                   (tap> params)
                   (tap> opts)
                   (tap> coeffects)
                   (throw e)))
               (throw (ex-info (str "Condition for event " (:id command) " is not met:\n"
                                    (:message error))
                               {:anomaly :incorrect})))))
         (merge params-error
                {:error (str "Params for command " (:id command) " do not meet spec")})))

     (catch Exception e
       (tap> command)
       (tap> params)
       (tap> opts)
       (throw e)))))
 
              
               
