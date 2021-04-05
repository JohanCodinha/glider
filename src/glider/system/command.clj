(ns glider.system.command
  (:require [malli.core :as  m]
            [malli.error :as me]
            [malli.util :as mu]
            [malli.transform :as mt]
            [sieppari.core :as s]))

(def command-store (atom {}))
@command-store

(defn register-command [command]
  (swap! command-store assoc (:id command) command ))

(defn run-coeffect [params [cofx-name coeffect]]
  [cofx-name (coeffect params)])

(defn sanitize-params [{schema :params} params]
  (let [sanitized (m/decode schema
                            params
                            mt/strip-extra-keys-transformer)
        valid? (m/validate schema
                           sanitized)]
    (if valid?
      sanitized
      {:system/error (me/humanize (m/explain schema sanitized))})))

(def inc-interceptor
  {:enter (fn [ctx] (tap> ctx)
            (update ctx :x inc))})

;; Simple handler, take `:x` from request, apply `inc`, and
;; return an map with `:y`.

(defn execute
  "Executes a queue of Interceptors attached to the context. Context must be a map. Interceptor is of shape [:name fn], returned value is added to context map and passed to downstream interceptors."
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
  "Orchestrate the execution of a command retrieved from the command store. "
  [command-id params]
  (if-let [command (get @command-store command-id)]
    (let [{params-error :system/error :as sanitized-params}
          (sanitize-params command params)]
      #_(println "sanitized-params\n"
                 sanitized-params)
      (if-not params-error
        (let [{coeffects :cofx} (execute {:params sanitized-params
                                    :execute/queue (:coeffects command)}
                                   :cofx)
              effect-return ((:effect command) {:cofx coeffects
                                                :params sanitized-params})]
          (when (:return command)
            ((:return command) {:cofx coeffects
                                :effect effect-return
                                :params sanitized-params})))
        (merge params-error
               {:error (str "Params for command " command-id " do not meet spec")})))
    {:error (str "No command with id" command-id)}))

(comment
  (def command-ex
    {:id ::import-user!
     :params [:and
              [:map
               [:userUid {:optional true}
                [:string]]
               [:username {:optional true}
                [:re {:error/message "User name can contain letters, numbers and underscores. The length must be between 6 and 15 characters."}
                 #"^[a-zA-Z_\d]{6,}$"]]]
              [:fn
               {:error/path [:userUid]
                :error/message "missing required key"}
               '(fn [{:keys [userUid username]}]
                  (or userUid username))]]
     :coeffects [[:user
                  (fn [{:keys [userUid username]}]
                    (get {"123" {:user "fifou"}} userUid)
                    #_(get-collaborator-by-legacy-Uid userUid)
                    #_(get-collaborator-by-username username))]]
     :effect (fn [cofx {:keys [userUid username]}]
               (str userUid "found " (:user cofx))
               #_(import-user-by-userUid! userUid)
               #_(if user-id
                   (import-user-by-username! username)))
     :return (fn [effect-return _]
               effect-return)
     :produce [:legacy-user-imported :legacy-user-updated]})
  (register-command command-ex)
  (sanitize-params command-ex {:username "13322*22" :userUid 1 :ok 1})
  (run! ::import-user! {:userUid "123" :username "codeforvic"})
  )
