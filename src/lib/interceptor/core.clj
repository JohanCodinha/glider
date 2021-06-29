(ns lib.interceptor.core
  (:require [clojure.core.async :as cca]
            [malli.core :as  m]
            [malli.error :as me]
            [malli.transform :as mt]
            [clojure.string :as str]))

(defn exception? [e]
  (instance? Exception e))

(defprotocol AsyncCtx
  (async? [t])
  (continue [t ctx f])
  (await [t]))

(extend-protocol AsyncCtx
  Object
  (async? [_] false)
  (continue [t f] (f t))
  (await [t] t))

(extend-protocol AsyncCtx
  clojure.lang.IDeref
  (async? [_] true)
  (continue [c ctx f] (let [c @c]
                        (if (exception? c)
                          (f (assoc ctx :error c))
                          (f c))))
  (await [c] @c))

(extend-protocol AsyncCtx
  clojure.core.async.impl.protocols.Channel
  (async? [_] true)
  (continue [c ctx f] (let [c (cca/<!! c)]
                        (if (exception? c)
                          (f (assoc ctx :error c))
                          (f c))))
  (await [c] (cca/<!! c)))

(def context-keys #{:interceptor/done :interceptor/queue :interceptor/tasks})

(defn exit [ctx]
  (assoc ctx :interceptor/queue []))

(def pause-interceptor
  {:name :pause
   :fn (fn [{p :pause queue :queue :as ctx}]
         (let [exit-ctx (dissoc (exit ctx) :pause)]
           (cond
             (and p (realized? p))
             (do (deliver @p exit-ctx)
                 exit-ctx)
             (empty? queue)
             (deliver p exit-ctx)
             :else ctx)))})

(defn inject
  "Inject interceptor into queue, add pause when :pause present in ctx"
  [ctx {name :name :as interceptor}]
  (let [queue (:interceptor/queue ctx)]
    (assoc ctx :interceptor/queue
           (vec (if (:pause ctx)
                  (cons [[nil :pause]  pause-interceptor] (cons [[nil name] interceptor] queue))
                  (cons [[nil name] interceptor] queue))))))

(def registry (atom {}))

(defn register-hander [name handler]
  (swap! registry assoc name handler))

(defn get-handler [name]
  (get @registry name))


(defn reg->cofx [[name & args]]
  (let [handler (get-handler name)]
    (if handler
      {:name name
       :fn (fn [ctx] (apply handler ctx args))}
      (throw
       (ex-info
        (str "No handler registered for :" name)
        {:name name
         :args args})))))

(defn cofx->interceptor
  "Wrap interceptor to work with cofx.
  pass in :params and :cofx, the wrapped function need to call the callback to assoc result to context. "
  [name f]
  {:name name
   :fn (fn [ctx]
         (if-not (get-in ctx [:cofx name])
           (let [res (await (f (select-keys ctx [:params :cofx])))]
             (if-not (exception? res)
               (assoc-in ctx [:cofx name] res)
               (throw res)))
           (throw
            (ex-info
             (str "Context already has a value in [:cofx " name "]")
             {:ctx ctx}))))})



(defn pause [p]
  (when-not (realized? p)
    (let [paused-ctx (promise)]
      (deliver p paused-ctx)
      @paused-ctx)))

(defn remove-processed [queue done]
  (let [done-indexed-set (into #{} done)
        queue-indexed queue]
    (remove #(done-indexed-set (first %1)) queue-indexed)))

(defn context? [m]
  (and (map? m)
       (some context-keys (keys m))))

(defn -try
  ([ctx f] (-try ctx f nil))
  ([ctx f error-handler]
   (if f
     (try
       (f ctx)
       (catch Exception e
         (if error-handler
           (-try (assoc ctx :interceptor/error e) error-handler)
           (assoc ctx :interceptor/error e))))
     ctx)))

(def debug-ctx (atom []))

(defn execute
  ([ctx] (execute nil ctx))
  ([old-ctx ctx]
   (def c ctx)
   (cond
     (async? ctx) (continue ctx old-ctx execute)
     (context? ctx)
     (let [queue (:interceptor/queue ctx)
           done (:interceptor/done ctx)
           [interceptor-key {fn :fn error-handler :error}] (first queue)]
       (println (str "Current interceptor: " interceptor-key))
       (swap! debug-ctx conj ctx)
       (if (or (:interceptor/error ctx)
               (not fn))
         ctx
         (let [next-ctx
               (-> ctx
                   (assoc :interceptor/queue ((comp vec rest) queue))
                   (assoc :interceptor/done ((fnil  conj []) done interceptor-key)))]
           (recur
            next-ctx
            (-try next-ctx fn error-handler)))))
     :else (throw
            (ex-info
             (str "Unsupported Context :" ctx " returned by "
                  (last (:interceptor/done old-ctx)))
             {:ctx ctx
              :old-ctx old-ctx})))))
(context? c)


(defn dispatch
  [tasks- registry ctx]
  (reset! debug-ctx [])
  (let [tasks (mapv (fn [t]
                     (let [[name params] t]
                       ((get registry name) params)))
                   tasks-)
        _ (tap> tasks)
        tasks* (map-indexed
                (fn [i {name :name :as interceptor}] [[i name] interceptor])
                tasks)
        ctx* (if (seq (:interceptor/done ctx))
               (assoc ctx :interceptor/queue
                      (if (:pause ctx)
                        (interleave
                         (remove-processed tasks* (:interceptor/done ctx))
                         (repeat [[nil :pause] pause-interceptor]))
                        (remove-processed tasks* (:interceptor/done ctx))))
               (assoc ctx :interceptor/queue
                      (if (:pause ctx)
                        (interleave
                         tasks*
                         (repeat [[nil :pause] pause-interceptor]))
                        tasks*)))]
    (execute ctx*)))

(defn resume [result task]
  (let [pause (promise)]
    (future (dispatch task result))
    pause))



