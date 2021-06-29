(ns glider.api.operation.routes
  (:require [glider.system.operation.core :as operation]))

(defn get-operation
  [db]
  (fn [req]
    (operation/find-by-id db (-> req :parameters :path :id))))

(defn pause-operation
  [db]
  (fn [req]
    (operation/pause! db (-> req :parameters :path :id))))

(defn resume-operation
  [db]
  (fn [req]
    (operation/resume! db (-> req :parameters :path :id))))

(defn routes
  [env]
  (let [db (:db env)]
    [["/operation/:id"
      {:get {:summary "Get status of operation"
             :tags ["Operation"]
             :responses {200 {:description "Operation current status"
                              :body [:map]}}
             :parameters {:path [:map [:id :uuid]]}
             :handler (get-operation db)}}]
     ["/operation/pause/:id"
      {:post {:summary "Get status of operation"
             :tags ["Operation"]
             :responses {200 {:description "Operation current status"
                              :body [:map]}}
             :parameters {:path [:map [:id :uuid]]}
              :handler (pause-operation db)}}]
     ["/operation/resume/:id"
      {:post {:summary "Get status of operation"
             :tags ["Operation"]
             :responses {200 {:description "Operation current status"
                              :body [:map]}}
             :parameters {:path [:map [:id :uuid]]}
             :handler (resume-operation db)}}]
     ]))
