(ns glider.wrapper.js-parser
  (:require [clj-http.client :refer [request] :as http]))

(defn parse-js-object [s]
  "Send js object to a node process and get json back"
  (-> (http/post "http://localhost:3001" {:body s :as :json})
      :body
      first))
