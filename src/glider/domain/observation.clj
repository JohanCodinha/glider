(ns glider.domain.observation)

(defn aggregate-root [events command]
   (reduce merge events))
