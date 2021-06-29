(ns async
  (:require [clojure.core.async :as cca]
            [glider.system.operation.core :as operation]
            [manifold.deferred :as d]
            [clj-http.client :as http :refer [request]]))


(comment
  (defn <make-request [sleep chan]
    (http/request
     {:method :get
      :url (str "http://httpstat.us/200?sleep=" sleep)
      :async true}
     (fn [res] (cca/put! chan res))
     (fn [exception] (println "exception message is: " (.getMessage exception))))
    chan)

  (defn <block [sleep]
    (cca/thread (Thread/sleep sleep)
                (System/currentTimeMillis))
    )

  (def r
    (let [now (System/currentTimeMillis)
          res (for [_ (range 2000)]
                      (<block 10000))]
      (->>
       (cca/pipe (cca/merge res) (cca/chan 1 (map #(- % now))))
       (cca/reduce conj []))))

  (time (cca/<!! r))

  (defn expensive-call [m]
    (Thread/sleep 2000) m)

  (->> [{:a 1} {:b 2} {:c 3} {:d 4}]
       (map (fn [m]
              (cca/thread
                (expensive-call m)))) ; creates a thread per call
       (cca/merge)                   ; merges the 4 chans returned into 1
       (cca/reduce merge {})         ; reduces items in chan with merge
       (cca/<!!)))





(comment
  (def post-num (atom 1))
  (def get-page [:page (fn [{sub :sub}]
                         (http/get (str "www.reddit.com/r/" sub ".json")))
                 :sub])

  (def get-sub [:sub (fn [_]
                       (Thread/sleep 1000)
                       (first (shuffle ["clojure" "javascript" "france" "europe"])))])

  (def get-post-num [:post-num (fn [_]
                                 @post-num)])

  (def reddit-post
    [get-post-num
     get-sub
     get-page])

  (def t {:a (future (do (Thread/sleep 2000) 1))
          :b (future 2)
          :c 4})

  (defn ^:dynamic *cookie-out-of-date* [msg info]
    (throw (ex-info msg info)))

  (def ^:dynamic *use-value*)

  (defn get-stuff [s]
    (if (= :ok s)
      s
      (binding [*use-value* identity]
        (*cookie-out-of-date* "Could not fetch mode" {:val s}))))

  (defn transform [s]
    (name (get-stuff s)))

  (defn get-s [s]
    (binding [*cookie-out-of-date*
              (fn [msg info]
                (println info)
                (if (= :ko (:val info))
                  (*use-value* (:val info))
                  :nope))]
      (transform s)))

  (get-stuff :stuff)
  (get-s :koo)

  (defn t [a]
    (/ 0 0)
    (throw (ex-info "trowing" {:in a})))

  (+ 1 (try
         (t 2)
         (catch clojure.lang.ExceptionInfo e
           (println (ex-data e)) (:in (ex-data e)))))
  
)

(comment

  (.. Runtime getRuntime availableProcessors)

  (def c1 (cca/chan))
  (let [c2 (cca/chan)]
    (cca/thread (while true
                  (let [[v ch] (cca/alts!! [c1 c2])]
                    (println "Read" v "from" ch))))
    (cca/>!! c1 "hi")
    (cca/>!! c2 "there"))

  (defn http-call
    []
    (Thread/sleep 800)
    {:status :ok
     :body (rand-int 10)})

  (http-call)

  (def state (atom []))
  (def operation (atom {}))
  (def resume (cca/chan))

  (defn job
    [x]
    (dotimes [_ x]
      (when (:paused @operation)
        (cca/<!! resume))
      (let [res (http-call)]
        (println res)
        (swap! state conj (http-call)))))

  (future (job 1000))

  (reset! operation {:paused true})

  (do (reset! operation {:pause false}) (cca/>!! resume :ok))

  (defn fake-fetch []
    (cca/thread
      (/ 0 0)
      (Thread/sleep 1000)
      "Ready!"))

;; returns immediately, prints "Ready!" after 5 secs
  (let [c (fake-fetch)]
    (cca/go (println (cca/<! c))))

  (def a (cca/chan 1))
  (def b (cca/chan 1))
  (def c (cca/chan 1))
  (def x (cca/pipeline 1 b (filter :ok) c (fn [error] (println "ahhh: " (.getMessage error)))) )
  (cca/pipe a c)
  (cca/go-loop [] (println "ready to wait a")(println (cca/<! a)) (recur))
  (cca/go-loop [] (println "ready to wait c")(println (cca/<! c)) (recur))
  (cca/>!! a :p)
  (cca/<!! d)
  d
  )
