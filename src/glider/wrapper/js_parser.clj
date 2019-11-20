(ns glider.wrapper.js-parser
  (:require [clj-http.client :refer [request] :as http]))

(defn parse-js-object [s]
  "Send js object to a node process and get json back"
  (-> (http/post "http://localhost:3001" {:body s #_ #_ :as :json-string-keys})
      #_:body
      #_first))

(defn key-string->keyword [m]
  (clojure.walk/prewalk
    m
    (fn [node]
      (if (map? node)
        (into {} (map
                   (fn [[k v :as item]]
                     (if (string? k)
                       (keyword (clojure.string/replace k #"\\s" "_"))
                               ))
                   node))
        node))))

(def data
  {"test" {"bar foo" 1}
   "foo bar baz" 2
   {"foo" 2} false}
  )
