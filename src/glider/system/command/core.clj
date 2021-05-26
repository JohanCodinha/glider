(ns glider.system.command.core
  (:require [malli.core :as  m]
            [malli.error :as me]
            [malli.util :as mu]
            [malli.transform :as mt]
            [sieppari.core :as s]))

(defn sanitize-params [{schema :params} params]
  (let [sanitized (m/decode schema
                            params
                            mt/strip-extra-keys-transformer)
        valid? (m/validate schema
                           sanitized)]
    (if valid?
      sanitized
      {:system/error (me/humanize (m/explain schema sanitized))})))


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


(defn run!
  "Orchestrate the execution of a command retrieved from the command store."
  ([command params]
   (run! command params {}))
  ([command params {run-coeffects :coeffects
                    run-handler :handler
                    run-side-effects :side-effects
                    environment :environment
                    :or {run-coeffects true run-handler true run-side-effects true}}]
   (let [{params-error :system/error :as sanitized-params}
         (sanitize-params command params)]
     (if-not params-error
       (let [{coeffects :cofx} (when run-coeffects
                                 (execute {:params sanitized-params
                                           :execute/queue (:coeffects command)}
                                          :cofx))
             effects (when run-handler
                       ((:effects command) {:cofx coeffects
                                            :params sanitized-params}))
             side-effects (when run-side-effects
                            (let [effects (map
                                           (fn [[effect-name effect-payload]]
                                             [effect-name ((get (:handler command) effect-name)
                                                           effect-payload
                                                           environment)])
                                           effects)]
                              (when-not (empty? effects)
                                (into {} effects))))]
         (when (:return command)
           ((:return command) (cond-> {:params sanitized-params}
                                run-coeffects (assoc :cofx coeffects)
                                run-handler (assoc :handler effects) 
                                run-side-effects (assoc :side-effects side-effects)))))
       (merge params-error
              {:error (str "Params for command " (:id command) " do not meet spec")})))))
