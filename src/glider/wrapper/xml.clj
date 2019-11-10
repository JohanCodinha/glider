(ns glider.wrapper.xml
  (:require #_[clojure.data.xml :refer [emit-str]]
            [clojure.xml :refer [parse] :as xml]
            [clojure.java.io :refer [input-stream]])
  (:import [javax.xml.parsers SAXParserFactory]))

(defn- non-validating [s ch]
  (..
   (doto
    (SAXParserFactory/newInstance)
     (.setFeature
      "http://apache.org/xml/features/nonvalidating/load-external-dtd" false))
   (newSAXParser)
   (parse s ch)))

(defn parse-xml [file-path]
  "This fn does the right thing"
  (parse (input-stream file-path) non-validating))

(comment (parse-xml "resources/user/user-details.xml"))

