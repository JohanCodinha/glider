(ns glider.db
  (:require [clj-postgresql.core :as pg]
            [clj-postgresql.types]
            [honeysql.core :as sql]
            [honeysql.helpers :as helpers]
            [clojure.java.jdbc :as jdbc]
            [integrant.core :as ig]))

(def datasource (atom nil))

(defn select [sql]
  (jdbc/query @datasource sql)) 

(defn insert! [table row-map]
  (jdbc/insert! @datasource table row-map))

(defmethod ig/init-key ::datasource [_ config]
  (let [ds (apply pg/spec (mapcat identity config))]
    (reset! datasource ds)
    (prn ::datasource @datasource)
    @datasource))

(comment
  (jdbc/execute! @datasource ["CREATE TABLE events ( stream_id varchar NOT NULL, uuid uuid, data jsonb)"])

  (jdbc/query db ["SELECT ?::json AS jsonobj" {"foo" "bar"}])
  (jdbc/query db ["SELECT * FROM events WHERE data ? 'bar'"])

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
