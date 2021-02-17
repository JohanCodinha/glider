(ns glider.domains.legacy.wrapper.xml
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

(defn parse-xml
  "This fn does the right thing"
  [file-path]
  (parse (input-stream file-path) non-validating))

(comment (parse-xml "resources/user/user-details.xml"))

