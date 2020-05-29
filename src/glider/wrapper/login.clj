(ns glider.wrapper.login
  (:require [clj-http.client :refer [request] :as http]
            [clojure.data.xml :refer [emit-str]]
            [diehard.core :as dh]
            [glider.wrapper.utils
             :refer [http-post-request process-request]]))

(defn login-request
  [username password]
  {:method :post
   :url "https://vba.dse.vic.gov.au/vba/login"
   :decode-cookies false
   :form-params
   {:username username
    :password password}})

(defn extract->cookie [http-response]
  "Login user and return cookie string from Set-Cookie header"
  (-> http-response
      (get-in [:headers "Set-Cookie"])
      first
      (clojure.string/split #";")
      first))

(defn login->cookie [username password]
  "Login user and return cookie string from Set-Cookie header"
  (-> (login-request username password)
      request
      extract->cookie))

(defn user-details-transaction []
  (let [transaction
        {:tag :transaction,
         :attrs
         {:xsi:type "xsd:Object",
          :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"},
         :content
         [{:tag :operations,
           :attrs {:xsi:type "xsd:List"},
           :content
           [{:tag :elem,
             :attrs {:xsi:type "xsd:Object"},
             :content
             [{:tag :criteria, :attrs {:xsi:type "xsd:Object"}}
              {:tag :operationConfig,
               :attrs {:xsi:type "xsd:Object"},
               :content
               [{:tag :dataSource, :content ["UserSessionDetail_DS"]}
                {:tag :operationType, :content ["fetch"]}]}
              {:tag :appID, :content ["builtinApplication"]}
              {:tag :operation, :content ["UserSessionDetail_DS_fetch"]}
              {:tag :oldValues, :attrs {:xsi:type "xsd:Object"}}]}]}]}]
    (emit-str transaction)))

(defn get-user-details [cookie]
  (let [options (http-post-request (user-details-transaction) cookie)]
    (-> options
        process-request
        (select-keys ["userUid"
                      "admin"
                      "batchUploadEnabled"
                      "displayName"
                      "username"]))))

;; user-details available keys
; "userRole"
; "gisUrl"
; "accountNonExpired"
; "credentialsNonExpired"
; "totalSiteRows"
; "expertReviewer"
; "largeAreaSiteForReport"
; "enabled"
; "authorities"
; "fullAccess"
; "viewer"
; "needsToChangePassword"
; "needsToAcceptTC"
; "accountNonLocked"
; "password"
; "expertReviewerOrAbove"
; "dataPublicationDate"
; "contributor"
; "userUid"
; "taxonManager"

