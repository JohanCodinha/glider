(ns glider.db
  (:require [clojure.string :as s]
            [next.jdbc :as jdbc]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs]
            [next.jdbc.sql :as jdbc.sql]
            [clj-postgresql.core :as pg]
            [clj-postgresql.types]
            [cheshire.core :as json]
            #_[honeysql.core :as sql]
            #_[honeysql.helpers :as helpers]
            [integrant.core :as ig])
  (:import [org.postgresql.util PGobject]
           [java.sql PreparedStatement]))

(defn parse-key [k]
  (if (s/starts-with? k ":")
    (-> k rest s/join keyword)
    k))

(defn encode-key [k]
  (if (keyword? k)
    (str k)
    k))

(def ->json #(json/generate-string % {:key-fn encode-key}))
(def <-json #(json/parse-string % parse-key))


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
    (if (#{"jsonb" "json"} type)
      (with-meta (<-json value) {:pgtype type})
      value)))

(set! *warn-on-reflection* true)

;; if a SQL parameter is a Clojure hash map or vector, it'll be transformed
;; to a PGobject for JSON/JSONB:
(extend-protocol prepare/SettableParameter
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

(def datasource (atom nil))

(defn select [sql]
  (jdbc.sql/query @datasource sql)) 

(defn insert! [table row-map]
  (jdbc.sql/insert! @datasource table row-map))

(defn instert-multi! [table row-maps]
  )

(defmethod ig/init-key ::datasource [_ config]
  (let [ds (apply pg/spec (mapcat identity config))]
    (reset! datasource ds)
    (prn ::datasource @datasource)
    @datasource))

(comment
  (java.util.UUID/fromString)
  (jdbc/execute! @datasource ["CREATE TABLE events ( stream_id varchar NOT NULL, version smallint NOT NULL, stream_id uuid NOT NULL, created_at timestamp default current_timestamp, payload jsonb NOT NULL, metadata jsonb)"])
  (jdbc/execute! @datasource ["DROP TABLE events"])
  (jdbc/query @datasource ["SELECT ?::json AS jsonobj" {"foo" "bar"}])
  (select ["SELECT * FROM events WHERE data #> '{nope}' = ?" 1])

  (def res (select ["SELECT * FROM events WHERE uuid = ?" (java.util.UUID/fromString "bbaf95cb-b9c5-4bf5-871b-02c2340fea32")]))

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
