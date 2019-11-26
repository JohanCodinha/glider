(ns user
  (:require [glider.system :as system]
            [camel-snake-kebab.core :as csk]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [integrant.repl :as ig-repl]
            [clojure.data.xml :refer [emit-str]]
            [clj-http.client :refer [request] :as http]
            [glider.wrapper.utils
             :refer [http-post-request
                     send-request
                     paginate-xml
                     parse-xml-file
                     page-stream]
             :as utils]
            [glider.wrapper.login :as login]
            [glider.wrapper.survey :as survey]
            [glider.wrapper.lookup :as lookup]
            [glider.wrapper.project :as project]
            [glider.wrapper.site :as site]
            [glider.wrapper.general-obs :as general-obs]))

(ig-repl/set-prep! (fn [] system/system-config))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(comment
  (go)
  (halt)
  (reset)
  (reset-all))

(comment
  (def admin_username (System/getenv "admin_username"))
  (def admin_password (System/getenv "admin_password"))
  (def mel_username (System/getenv "mel_username"))
  (def mel_password (System/getenv "mel_password"))
  (def cookie ((memoize login/login->cookie)
               admin_username admin_password))
  (def mel_cookie ((memoize login/login->cookie)
               mel_username mel_password))
  (try 
    (-> mel_cookie
        login/get-user-details)
    (catch Exception e
      (println e)))

  (def cfaobs (general-obs/get-user-general-obs cookie)) 

  {:contributors "johan codinha"
   :modifiedTsp 1499912034725
   :siteNme "user location"
   :surveyId 1394300
   :surveyStartSdt "2017-05-27T14:00:00.000Z"
   :expertReviewStatusCde "del"}

  (def cfasurvey (survey/get-survey 1394300 cookie))
  ;get-survey return full site information

  (with-out-str
    (pprint
      (dissoc cfasurvey :project)))

  {:primary-discipline-cde :discipline
   :date-accuracy-cde :date-accuracy 
   :monitoring-protocol-cde :monitoring-protocol
   :expert-review-status-cde :expert-review}

(spit "resources/lookup.txt" (lookup/lookups cookie))

(spit "resources/lookup-table.edn" (pr-str lo))

(read-string (slurp "resources/lookup-table.edn"))

(def lo (utils/lookups mel_cookie))

(spit "resources/lookup.")
(lookup/resolve-key (dissoc cfasurvey :project) lo)

(-> (lookup/resolve-key (dissoc cfasurvey :project) lo)
    :site
    :site-id)
936419


;; Project
(def project (project/get-project 3556 cookie))
(= (project/get-project 3556 cookie) (project/get-project 3556 mel_cookie) )
(def project-surveys (project/get-project-surveys 5742 cookie))

(lookup/resolve-key project5756 lo)

(spit "resources/lookup-cache.xml"
(dissoc lookup-cache :invalidate-cache :status :is-ds-response :data))

(def site-936419 (site/get-site 936419 cookie))

(= site-936419 (:site (lookup/resolve-key (dissoc cfasurvey :project) lo)))

)

