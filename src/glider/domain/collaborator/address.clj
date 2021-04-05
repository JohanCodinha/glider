(ns glider.domain.collaborator.address
  (:require [java-time :as time]
            [malli.core :as m]
            [malli.transform :as mt]))

(defn parse-timestamp [timestamp]
  (-> timestamp
      time/instant
      (time/zoned-date-time "Australia/Victoria")
      str))

(def Schema
  [:map
   [:primary boolean?]
   [:supplied-date {:optional true
                    :decode/time parse-timestamp} string?]
   [:street-name string?]
   [:street-number {:optional true} string?]
   [:country-name string?]
   [:postcode string?]
   [:city string?]
   [:state string?]])

(defn parse [input]
  (m/decode
   Schema
   input
   (mt/transformer {:name :time})))
