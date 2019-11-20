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
                     page-stream]]
            [glider.wrapper.login :as login]
            [glider.wrapper.survey :as survey]
            [glider.wrapper.lookup :as lookup]
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
  (def cookie ((memoize login/login->cookie)
               admin_username admin_password))
  (try 
    (-> cookie
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

  (with-out-str
    (pprint
      (dissoc cfasurvey :project)))

  {:primary-discipline-cde -> :lookup-type-txt "Discipline"
   :date-accuracy-cde -> :lookup-type-txt "Date Accuracy"
   :monitoring-protocol-cde -> :lookup-type-txt "Monitoring Protocol"
   :expert-review-status-cde -> :lookup-type-txt "Expert Review"

(spit "resources/lookup.txt" (lookup/lookups cookie))

(def lo (lookup/lookups cookie))

(count lo)

(into #{} (map :lookup-type-txt lo))

(->> (filter #(= "Discipline" (:lookup-type-txt %)) lo)
   (map :lookup-desc))
("Terrestrial fauna" "Aquatic fauna" "Flora" "Aquatic invertebrates" "Marine")
(->> (filter #(= "SC Discipline" (:lookup-type-txt %)) lo)
   (map :lookup-desc))
("Terrestrial fauna" "Flora" "Marine" "Aquatic invertebrates" "Aquatic fauna" "All disciplines")


(keys lookup-cache)
(:lookup-sampling-method-lut-af
:lookup-incidental-obs-type
:lookup-taxon-level
:lookup-reliability
:lookup-sampling-method-lut-ai
:invalidate-cache
:lookup-conservation-status
:lookup-project-status
:lookup-count-accuracy
:lookup-extra
:lookup-batch-upload-status
:lookup-cover-abundance
:lookup-other-agency
:lookup-project-user-type
:lookup-taxon-type
:lookup-sc-discipline
:lookup-sampling-method-lut-fl
:lookup-type
:lookup-published-status
:lookup-sampling-method-lut-all
:lookup-sampling-method-lut-ma
:lookup-sampling-method-lut-tf
:lookup-date-accuracy
:lookup-discipline)

(:lookup-sc-discipline lookup-cache)



(spit "resources/lookup-cache.xml"
(dissoc lookup-cache :invalidate-cache :status :is-ds-response :data))

[{:depend-on-cde nil, :enabled true, :id "tf", :label "Terrestrial fauna", :type "SC Discipline"
  :depend-on-cde nil, :enabled true, :id "fl", :label "Flora", :type "SC Discipline"
  :depend-on-cde nil, :enabled true, :id "af", :label "Aquatic fauna", :type "SC Discipline"
  :depend-on-cde nil, :enabled true, :id "ai", :label "Aquatic invertebrates", :type "SC Discipline"
  :depend-on-cde nil, :enabled true, :id "ma", :label "Marine", :type "SC Discipline"
  :depend-on-cde nil, :enabled true, :id "all", :label "All disciplines", :type "SC Discipline"}]

)

