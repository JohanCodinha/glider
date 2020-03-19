(ns glider.system
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]
            [glider.api.handler :as ring-handler]))

(def system-config
  {:glider/jetty {:port 8080 :handler (ig/ref :glider/api)}
   :glider/api {:db (ig/ref :glider/postgresql)}
   :glider/postgresql nil})

(defmethod ig/init-key :glider/jetty [_ {:keys [handler port]}]
  (prn handler)
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/init-key :glider/api [_ {:keys [db]}]
  (ring-handler/create-app db))

(defmethod ig/init-key :glider/postgresql [_ _]
 nil)

(defmethod ig/halt-key! :glider/jetty [_ jetty]
  (.stop jetty))

(defn -main []
  (ig/init system-config))

(comment
  (defn start []) 
  (def system (ig/init system-config))
  (ig/halt! system)
  )
