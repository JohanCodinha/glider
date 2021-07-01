(ns interceptor
  (:require [clojure.core.async :as cca]
            [manifold.deferred :as d]
            [jsonista.core :refer [read-value]]
            [clj-http.client :as http :refer [request]]
            [lib.interceptor.core :as i]
            [lib.interceptor.standard :refer [condition-check->interceptor parse-params]]))

(i/register-hander
 :fetch-api
 (fn [ctx req]
   
   (let [response
         (read-value
          (:body (request (assoc req :url
                                 (or (:url req) (:base-url req))))))
         fetched (get-in ctx [:cofx :fetch-api])
         after (get-in response ["data" "after"])]
     (if (and (< (count fetched) 5) after)
       (-> ctx
           (update-in [:cofx :fetch-api] #((fnil conj []) % response))
           (i/inject (let [{name :name fn :fn}
                           (i/reg->cofx [:fetch-api (assoc req :url (str (:base-url req) "?after=" after))])]
                       [[nil name] fn])))
       (-> ctx
           (update-in [:cofx :fetch-api] #((fnil conj []) % response))
           (update-in [:cofx :fetch-api] (fn [res] (map #(get-in % ["data" "title"])
                                                        (mapcat #(get-in % ["data" "children"]) res)))))))))

(defn fetch-interceptor
  [req]
  {:name :fetch
   :fn
   (fn [ctx]
     (tap> ctx)
     (let [response
           (read-value
            (:body (request (assoc req :url
                                   (or (:url req) (:base-url req))))))
           fetched (get-in ctx [:cofx :fetch-api])
           after (get-in response ["data" "after"])]
       (tap> response)
       (if (and (< (count fetched) 5) after)
         (-> ctx
             (update-in [:cofx :fetch-api] #((fnil conj []) % response))
             (i/inject [:fetch
                        (assoc req :url (str (:base-url req) "?after=" after))]))
         (-> ctx
             (update-in [:cofx :fetch-api] #((fnil conj []) % response))
             (update-in [:cofx :fetch-api]
                        (fn [res] (map #(get-in % ["data" "title"])
                                       (mapcat #(get-in % ["data" "children"]) res))))))))})

(def registry
  {:fetch fetch-interceptor
   :website #(i/cofx->interceptor
              :website
              (fn [{{url :url} :params}]
                (future (Thread/sleep 2000)
                        (prn "fetched done")
                        (format % url))))
   :now (constantly
         (i/cofx->interceptor
          :now
          (fn [_]
            (Thread/sleep 1000)
            (System/currentTimeMillis))))
   :cls (constantly
         {:name :cls
          :fn (fn [{eff :eff :as ctx}]
                (update ctx :effects
                        #(apply conj (or %1 []) %2)
                        [[:console (str "hello-world :" eff)]]))})
   :side (constantly
          {:name :side
           :fn  (fn [{:as ctx}]
                  (update ctx :effects
                          #(apply conj (or %1 []) %2)
                          [[:console (str "Done " (count (:interceptor/done ctx)) " interceptor")]]))})})

(def tasks
  [[:website "{'a':1 'ur':%s}"]
   [:fetch {:method :get
            :base-url "https://www.reddit.com/r/Clojure.json"
            :headers {"User-agent" "clojure demo"}}]
   [:now]
   [:cls]
   [:side]])

(comment
  (let [uuid (java.util.UUID/randomUUID)]
    (get {uuid false} uuid))

  (def r
    (let [uuid (java.util.UUID/randomUUID)
          p (promise)
          result (future (i/dispatch tasks
                                     registry
                                     {:params {:url "http://www.reddit.com/r/Clojure.json"}
                                      :uuid uuid
                                      :pause p}))]
      #_#_(Thread/sleep 1000)
      (i/pause p)
      @result))

  ;;Need to do interceptor effect handler
  
  (def r2
    (let [p (promise)
          res (future (i/dispatch tasks
                                  registry
                                  (dissoc
                                   (assoc r :pause p)
                                   :interceptor/paused)))]

      (Thread/sleep 2000)
      [(i/pause p) res]))
  (dissoc (first r2) :cofx)

  (def paused @(second *1))

  (i/resume paused tasks)

  (def res (i/dispatch tasks  {:url "http://www.reddit.com/r/Clojure.json"
                               :uuid (java.util.UUID/randomUUID)}
                       (promise)))
  (def r (i/dispatch [(i/cofx->interceptor
                       :website
                       (fn [{{url :url} :params}]
                         (future (Thread/sleep 2000)
                                 (format  "{'a':1 'ur':%s}" url))))
                      {:name :crash
                       :fn (fn [ctx] (println "attempt login")
                             (throw (ex-info "login-error" {:type :login})))
                       :error (fn [{error :interceptor/error :as ctx}]
                                (if (= :login-fail (:type (ex-data error)))
                                  (-> ctx
                                      (dissoc :interceptor/error)
                                      (i/inject {:name :recover
                                                 :fn (fn [ctx] (println "recovering")
                                                       (future (assoc ctx :ok 1)))}))
                                  ctx))}]
                     {:params {:url "http://www.reddit.com/r/Clojure.json"}
                      :uuid (java.util.UUID/randomUUID)}
                     (promise))))


(fn [{:keys [url count]}]
  [[#(clojure.string/starts-with? url "http://") :invalid "Url is not valid"]
   [#(> count 5) :low "Count is bellow 5"]])


(comment
  (let [pause (promise)
        tasks [{:name :parse-params
                :fn parse-params}
               (i/cofx->interceptor
                :website
                (fn [{{url :url} :params}]
                  (future (Thread/sleep 2000)
                          (format  "{'a':1 'ur':%s}" url))))
               (condition-check->interceptor
                (fn [{:keys [url count]}]
                  [[#(clojure.string/starts-with? url "http://") :invalid "Url is not valid"]
                   [#(> count 5) :low "Count is bellow 5"]]))
               (i/cofx->interceptor
                :website2
                (fn [{{url :url} :params}]
                  (future (Thread/sleep 2000)
                          (format  "{'a':1 'ur':%s}" url))))]
        ctx {:params {:url "http://www.reddit.com/r/Clojure.json"
                      :extra :should-be-removed
                      :count "9"}
             :pause pause
             :params-schema [:map
                             [:url :string]
                             [:count :int]]
             :uuid (java.util.UUID/randomUUID)}]
    (future (i/dispatch tasks
                        ctx
                        ))
    (Thread/sleep 1000)
    (i/pause pause)
    
    )
  )
