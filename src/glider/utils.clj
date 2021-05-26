(ns glider.utils
  (:require [java-time :as time]))

(defn uuid [] (java.util.UUID/randomUUID))

(defn timestamp
  ([] (.truncatedTo (time/instant) java.time.temporal.ChronoUnit/MILLIS))
  ([n] (time/instant n)))
