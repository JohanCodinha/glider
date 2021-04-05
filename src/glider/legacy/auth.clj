(ns glider.legacy.auth
  (:require [clojure.string :as string]
            [clj-http.client :as http]))

(def admin-cookie (atom nil))

#_(defmethod ig/init-key ::admin-cookie [_ config]
  (let [ds (apply pg/spec (mapcat identity config))]
    (reset! datasource ds)
    (prn ::datasource @datasource)
    @datasource))

(defn login-request
  [username password]
  {:method :post
   :url "https://vba.dse.vic.gov.au/vba/login"
   :decode-cookies false
   :form-params
   {:username username
    :password password}})

(defn extract->cookie
  "Login user and return cookie string from Set-Cookie header"
  [http-response]
  (-> http-response
      (get-in [:headers "Set-Cookie"])
      first
      (string/split #";")
      first))

(defn login->cookie
  "Login user and return cookie string from Set-Cookie header"
  [username password]
  (-> (login-request username password)
      http/request
      extract->cookie))

(defn refresh-cookie []
  (let [admin-username (System/getenv "admin_username")
        admin-password (System/getenv "admin_password")
        cookie (login->cookie admin-username admin-password)]
    (reset! admin-cookie cookie)
    @admin-cookie))

(comment (refresh-cookie)
         (reset! admin-cookie "JSESSIONID=070FEB520FD5CC1A884A18B33CF6B3.worker1"))
