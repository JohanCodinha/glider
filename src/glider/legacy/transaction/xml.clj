(ns glider.legacy.transaction.xml
  (:require #_[clojure.data.xml :refer [emit-str]]
            [clojure.xml :refer [parse] :as xml]
            [clojure.java.io :refer [input-stream]]
            [clojure.java.io :as io])
  (:import [javax.xml.parsers SAXParserFactory]))

(defn- non-validating [s ch]
  (..
   (doto
    (SAXParserFactory/newInstance)
     (.setFeature
      "http://apache.org/xml/features/nonvalidating/load-external-dtd" false))
   (newSAXParser)
   (parse s ch)))

(defn parse-xml
  "This fn does the right thing"
  [file-path]
  (parse (input-stream (clojure.java.io/resource file-path)) non-validating))

(comment (parse-xml "resources/user/user-details.xml")

         (parse-xml "resources/lookup-user-role.xml")
        
         )

