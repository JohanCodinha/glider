(ns glider.domains.legacy.transaction.login
  #_(:require [clj-http.client :as http :refer [request]]
            [clojure.data.xml :refer [emit-str]]
            [diehard.core :as dh]
            [glider.domains.legacy.transaction.utils
             :refer
             [http-post-request process-request]]))

(defn user-details-transaction []
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
              {:tag :oldValues, :attrs {:xsi:type "xsd:Object"}}]}]}]})

#_ (defn get-user-details [cookie]
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

