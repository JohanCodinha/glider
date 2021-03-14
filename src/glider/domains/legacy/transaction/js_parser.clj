(ns glider.domains.legacy.transaction.js-parser
  (:require [clj-http.client :refer [post]]
            [cheshire.core :refer [parse-string]]))

(defn parse-json
  ([s] (parse-string s))
  ([s key-transform] (parse-string s key-transform)))

(defn parse-js-object!
  "Send js object to a node process and get json back"
  [s]
  (-> (post "http://localhost:3001" {:body s})
      :body
      parse-json))

