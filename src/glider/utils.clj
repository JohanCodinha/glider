(ns glider.utils
  (:require [java-time :as time]))

(defn uuid [] (java.util.UUID/randomUUID))

(defn timestamp
  ([] #_(.toEpochMilli (time/instant)) (time/instant))
  ([n] (time/instant n)))
