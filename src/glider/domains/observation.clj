(ns glider.domains.observation)

(defn aggregate-root [events command]
   (reduce merge events))
