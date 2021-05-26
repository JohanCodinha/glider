(ns async
  (:require [clojure.core.async :as cca]
            [clj-http.client :as http]))

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
