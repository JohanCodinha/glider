(ns glider.wrapper.js-parser
  (:require [clojure.string :as string]
            [camel-snake-kebab.core :as csk]
            [clj-http.client :refer [request] :as http]
            [cheshire.core :refer [parse-string]]))

(defn parse-json [s]
  (parse-string s
                (fn [k]
                  (csk/->kebab-case-keyword k))))

(defn parse-js-object [s]
  "Send js object to a node process and get json back"
  (-> (http/post "http://localhost:3001" {:body s})
      :body
      parse-json
      #_ :data
      #_ first))

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
