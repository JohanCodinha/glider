(ns glider.db
  (:require [clojure.string :as s]
            [next.jdbc :as jdbc]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs]
            [next.jdbc.sql :as jdbc.sql]
            [next.jdbc.date-time]
            [next.jdbc.quoted :as quoted]
            [clj-postgresql.core :as pg]
            [clj-postgresql.types]
            #_[honeysql.core :as sql]
            #_[honeysql.helpers :as helpers]
            [integrant.core :as ig]
            [clojure.string :as st]
            [medley.core :refer [map-keys]]
            [glider.utils :refer [timestamp]]
            [jsonista.core :as json]
            [jsonista.tagged :as jt])
  (:import [org.postgresql.util PGobject]
           [java.sql PreparedStatement]
           [java.sql Types])
  (:import (clojure.lang Keyword PersistentHashSet)))

(defn parse-key [k]
  (if (s/starts-with? k ":")
    (-> k
        (st/replace-first #"^." "")
        keyword)
    k))

(defn encode-key [k]
  (if (keyword? k)
    (str k)
    k))

(def mapper (json/object-mapper {:decode-key-fn parse-key
                                 :encode-key-fn encode-key
                                 :modules [(jt/module
                                            {:handlers {Keyword {:tag "!kw"
                                                                 :encode jt/encode-keyword
                                                                 :decode keyword}
                                                        PersistentHashSet {:tag "!set"
                                                                           :encode jt/encode-collection
                                                                           :decode set}}})]}))
(def ->json #(json/write-value-as-string % mapper))
(def <-json #(json/read-value % mapper))

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (tap> value)
    (tap> type)
    (if (#{"jsonb" "json"} type)
      (with-meta (<-json value) {:pgtype type})
      value)))


(with-meta (<-json "{\"a\":true}") {:pgtype "json"})

(set! *warn-on-reflection* true)

;; if a SQL parameter is a Clojure hash map or vector, it'll be transformed
;; to a PGobject for JSON/JSONB:
(extend-protocol prepare/SettableParameter
  clojure.lang.Keyword
  (set-parameter [k ^PreparedStatement s i]
    (.setObject s i (doto (PGobject.)
                      (.setType "varchar")
                      (.setValue (str k)))))

  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))

#_(extend-protocol cheshire.generate/JSONable
  java.time.Instant  
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))


(extend-protocol rs/ReadableColumn
  java.lang.String
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (if (st/starts-with? v ":")
      (keyword (st/replace-first v  #"^." ""))
      v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (if (st/starts-with? v ":")
      (keyword (st/replace-first v  #"^." ""))
      v)))

(def datasource (atom nil))

(next.jdbc.date-time/read-as-instant)

(extend-protocol rs/ReadableColumn
    java.sql.Date
    (read-column-by-label [^java.sql.Date v _]     v)
    (read-column-by-index [^java.sql.Date v _2 _3] v)
    java.sql.Timestamp
    (read-column-by-label [^java.sql.Timestamp v _]     (timestamp v))
    (read-column-by-index [^java.sql.Timestamp v _2 _3] (timestamp v)))

(extend-protocol prepare/SettableParameter
  ;; Java Time type conversion:
  java.time.Instant
  (set-parameter [^java.time.Instant v ^PreparedStatement s ^long i]
    (.setTimestamp s i (java-time/instant->sql-timestamp v)))
  java.sql.Timestamp
  (set-parameter [^java.sql.Timestamp v ^PreparedStatement s ^long i]
    (.setTimestamp s i v)))

(defn select!
  ([sql]
   (select! @datasource sql))
  ([db sql]
   (jdbc.sql/query db sql jdbc/unqualified-snake-kebab-opts))) 

(defn dash->underscore [kword]
  (-> kword
      str
      (st/replace "-" "_")
      (st/replace-first #"^." "")
      keyword))

(defn insert!
  ([table row-map]
   (insert! @datasource table row-map))
  ([db table row-map]
   (jdbc.sql/insert! db table row-map jdbc/unqualified-snake-kebab-opts)))

(defn insert-multi! [table cols rows]
  (jdbc.sql/insert-multi! @datasource table cols rows))

#_(defn insert-batch!
  "Given a connectable object, a table name, a sequence of column names, and a vector of rows of data (vectors of column values), inserts the data as multiple rows in the database."
  [table cols rows]
  (with-open [con (jdbc/get-connection @datasource)
              c (apply str (interpose "," (map name cols )))
              r (apply str (interpose "," (map (constantly "?") cols )))
              sql (str "insert into " (name table)
                       " (" c ")"
                       " values (" r ")")
              ps  (jdbc/prepare con [sql])]
    (prepare/execute-batch! ps rows)))

(defn execute!
  ([sql]
   (execute! @datasource sql))
  ([db sql]
   (jdbc/execute! db sql)))

(defn update!
  ([sql]))
;; utils
(defn events->rows-cols [events]
  [(->> events first keys) (map vals events)])

(defmethod ig/init-key ::datasource [_ datasource-options]
  (let [ds (apply pg/spec (mapcat identity datasource-options))]
    (reset! datasource ds)
    (prn ::datasource @datasource)
    @datasource))

(defmethod ig/halt-key! ::datasource [_ _ #_datasource]
  (reset! datasource nil))

(def tables
  {:authentication
   ["CREATE TABLE authentication (uuid uuid UNIQUE, legacy_uid varchar, login varchar UNIQUE, password varchar)"]
   :legacy-events
   ["CREATE TABLE legacy_events (id uuid NOT NULL UNIQUE, stream_id varchar NOT NULL, type varchar, version smallint NOT NULL, created_at timestamp default current_timestamp, payload jsonb NOT NULL)"]
   })
(comment
  (jdbc/execute! @datasource (:authentication tables))
  (jdbc/execute! @datasource (:legacy-events tables))
  
  (java.util.UUID/fromString)
  (jdbc/execute! @datasource ["CREATE TABLE events (id uuid NOT NULL, stream_id uuid NOT NULL, type varchar(50), version smallint NOT NULL, created_at timestamp default current_timestamp, payload jsonb NOT NULL, metadata jsonb)"])

  (jdbc/execute! @datasource ["CREATE TABLE legacy_events (id uuid NOT NULL UNIQUE, stream_id varchar(20) NOT NULL, type varchar(50), version smallint NOT NULL, created_at timestamp default current_timestamp, payload jsonb NOT NULL)"])

  {:id (uuid)
   :event-name :legacy-user-synchronized
   :aggregate-name :legacy-user
   :aggregate-id aggregate-id
   :created_at (timestamp)
   :version version
   :data data}
  
  (jdbc/execute! @datasource ["CREATE TABLE authentication (uuid uuid NOT NULL UNIQUE, login_name varchar(50) UNIQUE, password varchar(60))"])
  
  (jdbc/execute! @datasource ["DROP TABLE authentication"])
  (jdbc/execute! @datasource ["DROP TABLE legacy_events"])
  (jdbc/query @datasource ["SELECT ?::json AS jsonobj" {"foo" "bar"}])
  (select! ["SELECT * FROM events WHERE data #> '{nope}' = ?" 1])

  (def res (select! ["SELECT * FROM events WHERE uuid = ?" (java.util.UUID/fromString "bbaf95cb-b9c5-4bf5-871b-02c2340fea32")]))
  (select! ["SELECT * FROM events"])
  (-> res first :events/data)
  (prn @datasource) 
  ;

  (jdbc/insert! @datasource :events {:stream_id 1 :data {:bar :booze "list" [1 2 3]} })
  (jdbc/insert! spec :events {:stream_id 1 :data {:text "le petit matis""bar" false "list" [1 2 3]}})

  (jdbc/execute! db ["CREATE TABLE books (book_id serial NOT NULL, uuid uuid, data jsonb)"])

  (jdbc/insert! db :books
                {:book_id 1
                 :data {"title" "Sleeping Beauties", "genres" ["Fiction", "Thriller", "Horror"], "published" false}})

  (let [s (sql/format {:select [[(sql/raw "data->'list'") :list]]
                       :from [:events]
                       })]
    (jdbc/query db s))

  (jdbc/query @datasource ["SELECT * FROM books"])
  )
