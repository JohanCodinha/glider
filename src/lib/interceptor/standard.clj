(ns lib.interceptor.standard
  (:require [malli.core :as  m]
            [malli.error :as me]
            [malli.transform :as mt]))

(defn condition-check->interceptor [f]
  {:name :condition-check
   :fn (fn [{params :params :as ctx}]
         (let [conditions-anomaly (reduce (fn [_ [condition? anomaly message]]
                                            (if (condition?)
                                              nil
                                              (reduced {:anomaly anomaly
                                                        :message message})))
                                          nil
                                          (f params))]
           (if conditions-anomaly
             (assoc ctx :interceptor/error conditions-anomaly)
             ctx)))})

(defn parse-params [{schema :params-schema params :params :as ctx}]
  (if schema
    (let [sanitized (m/decode schema
                              params
                              (mt/transformer
                               mt/strip-extra-keys-transformer
                               mt/string-transformer))
          valid? (m/validate schema
                             sanitized)]
      (tap> sanitized)
      (tap> valid?)
      (if valid?
        (assoc ctx :params sanitized)
        (assoc ctx :interceptor/error (me/humanize (m/explain schema sanitized)))))
    ctx))
