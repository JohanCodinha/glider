(ns glider.utils
  (:require [java-time :as time]))

(defn uuid
  ([] (java.util.UUID/randomUUID))
  ([uuid-str] (java.util.UUID/fromString uuid-str)))

(defn timestamp
  ([] (.truncatedTo (time/instant) java.time.temporal.ChronoUnit/MILLIS))
  ([n] (time/instant n)))
