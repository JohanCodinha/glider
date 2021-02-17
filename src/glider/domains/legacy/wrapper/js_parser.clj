(ns glider.domains.legacy.wrapper.js-parser
  (:require [clojure.string :as string]
            [camel-snake-kebab.core :as csk]
            [clj-http.client :refer [request] :as http]
            [clojure.walk :refer [prewalk]]
            [cheshire.core :refer [parse-string]]))

(defn kebab-case-keyword [k]
  (csk/->kebab-case-keyword k))

(defn parse-json
  ([s] (parse-string s))
  ([s key-transform] (parse-string s key-transform)))

(defn parse-js-object
  "Send js object to a node process and get json back"
  [s]
  (-> (http/post "http://localhost:3001" {:body s})
      :body
      parse-json))

(defn key-string->keyword [m]
  (prewalk
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

(comment

  (parse-js-object "test")
  (def input-test (first (clojure.string/split-lines (slurp (clojure.java.io/resource "test-input-vba-xhr-long.txt")))))
  (def res (parse-json (:body (http/post "http://localhost:3000" {:body input-test
                                                                 :as :auto})))))


