(ns glider.system
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]
            [clj-postgresql.core :as pg]
            [clj-postgresql.types]
            [glider.api.router :refer [ring-handler]]))

(def system-config
  {:glider/jetty {:port 8080 :handler (ig/ref :glider/api)}
   :glider/api {:db (ig/ref :glider.db/datasource)}
   
   :glider.db/datasource {:host "localhost"
                          :user "sugar"
                          :dbname "glider"
                          :password "surfing"
                          :reWriteBatchedInserts true}})

(defmethod ig/init-key :glider/jetty [_ {:keys [handler port]}]
  (prn "running jetty" port)
  (jetty/run-jetty (handler) {:port port :join? false :async true}))

(defmethod ig/init-key :glider/api [_ {:keys [db]}]
  #(ring-handler {:db db}))

(defmethod ig/halt-key! :glider/jetty [_ jetty]
  (.stop jetty))

(defn -main []
  (ig/init system-config))

(comment
  (def system (ig/init system-config))
  (ig/halt! system))
