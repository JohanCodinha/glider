(ns glider.api.legacy.user.handler
  (:require 
            [glider.legacy.users :refer [import-user-command]]
            [glider.system.command.core :as command]))

(defn fetch-by-userUid [db]
  (fn [req]
    (println "Vba sync requested for:"
             (-> req :parameters :body :userUid))
    (let [command-return
          (command/run! import-user-command
                        (-> req :parameters :body)
                        {:side-effects true
                         :environment {:db db}})]
      {:status 200
       :body {:command
              {:current command-return}}})))
