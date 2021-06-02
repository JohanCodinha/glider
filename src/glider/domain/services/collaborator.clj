(ns glider.domain.services.collaborator
  (:require [crypto.password.bcrypt :as crypto]
            [glider.db :refer [select! insert! execute!]]))

(def Schema
  [:map
   [::uuid uuid?]
   [::password string?]
   [::login-name string?]])

(defn fetch-credentials
  [{:keys [login-name uuid]}]
  (first
   (select!
     ["SELECT * FROM authentication WHERE uuid = ? OR login_name = ?"
      uuid login-name])))


(defn persist-authentication-credentials!
  [{:keys [uuid password login-name] :as credentials}]
  (let [exist (fetch-credentials credentials)]
    (when exist
      (execute! ["DELETE FROM authentication WHERE uuid = ? OR login_name = ?"
                     uuid login-name]))
    (insert! :authentication
             {:uuid uuid :password (crypto/encrypt password) :login_name login-name})))

(defn valid-credentials?
  [{:keys [password login-name uuid] :as credentials}]
  (let [{encrypted-passwrod :authentication/password}
        (fetch-credentials credentials)]
    (crypto/check password encrypted-passwrod)))

(comment
  (def uuid "1234")
  (valid-credentials? {:password "1234" :uuid uuid})

(execute! ["DELETE FROM authentication WHERE uuid = ? OR login_name = ?"
                     uuid nil])

(persist-authentication-credentials!
 {:uuid uuid :password "1234" :login-name "codeforvic"})
(def uuid (java.util.UUID/randomUUID))
(insert! :authentication {:uuid uuid :password "hashedpw" :login_name "codeforvic"})

(select! ["SELECT * FROM authentication WHERE login_name = ?" "codeforvic"])
(select! ["SELECT * FROM authentication WHERE uuid = ?" uuid])

(def encrypted (crypto/encrypt "foobars"))
encrypted
(take 100  (map (comp crypto/encrypt str) (range)))
(map str (range 10))
(crypto/check "foobar" encrypted))
