(ns user
  (:require [glider.system :as system]
            [clj-fuzzy.metrics :as fuzzy]
            [camel-snake-kebab.core :as csk]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [integrant.core :as ig]
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
            [glider.wrapper.users :as users]
            [glider.wrapper.site :as site]
            [glider.wrapper.general-obs :as general-obs]))

(defn trace! [v]
  (let [m    (meta v)
        n    (symbol (str (ns-name (:ns m))) (str (:name m)))
        orig (:trace/orig m @v)]
    (alter-var-root v (constantly (fn [& args]
                                    (prn (cons n args))
                                    (apply orig args))))
    (alter-meta! v assoc :trace/orig orig)))

(defn untrace! [v]
  (when-let [orig (:trace/orig (meta v))]
    (alter-var-root v (constantly orig))
    (alter-meta! v dissoc :trace/orig)))

(ig-repl/set-prep! (fn [] system/system-config))

(ig/load-namespaces system/system-config)

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
  (def active-users (users/get-active-users cookie))
  ;get a record
  ;save it to db

  (def a-users
    (transduce 
      (comp (take 1)
            (map #(doto % utils/fetched-rows-report)))
      conj []
      (users/get-active-users! cookie)
      ))

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
  (def cfb (survey/get-survey 1394300 cookie))

  (use 'clojure.data)
  
  (= cfasurvey cfb)

  (diff (update-in cfasurvey [:project] #(dissoc % :organisations))
        (update-in cfb [:project] #(dissoc % :organisations)))
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

  (def cookie (login/login->cookie "melvba" "vba123"))
  ;; Project
  (def project (project/get-project 3556 cookie))
  (= (project/get-project 3556 cookie) (project/get-project 3556 mel_cookie) )
  (def project-surveys (project/get-project-surveys 5742 cookie))


  (spit "resources/lookup-cache.xml"
        (dissoc lookup-cache :invalidate-cache :status :is-ds-response :data))

  (def site-936419 (site/get-site 936419 cookie))

  ;; All projects
  (def all-projects (project/get-projects cookie))
  (prn all-projects)
  (def project-surveys (project/get-project-surveys 540 cookie))
  (def site-details (survey/get-site-details 1207405 cookie))

  (def sd (survey/get-site-details 411512 cookie))

  (defn get-all-keys [m]
    (flatten
      (map (fn [[k v]]
             (if (map? v)
               [k (get-all-keys v)] 
               [k])
             )
           m)))

  (defn keyword-ending-by-cde [k]
    (re-find #"-cde$" (name k)))

  (filter keyword-ending-by-cde (get-all-keys site-details))
  (filter keyword-ending-by-cde (get-all-keys site-details))
  (:private-land-cde site-details)
  (def test-cde 
    [:water-body-type-cde
     :wetland-category-cde
     :wetland-other-class-cde
     :river-basin-cde
     :surface-soil-texture-cde
     :site-shape-cde
     :open-closed-cde
     :geology-cde
     :landform-cde
     :private-land-cde
     :restricted-flag-cde])
  (def lookup-table (utils/get-lookups cookie))

  (def lookup-table
    (read-string (slurp "resources/lookup-table.edn")))

:lookup-id 1197
:lookup-cde "food"
:lookup-src "LOOKUP_EXPANSION"
:sort-order 3
:creation-tsp 1317218400000
:lookup-desc "Food item"
:lookup-type-txt "Aboriginal"
:lookup-status-txt nil
:modified-tsp


(fuzzy/levenshtein (name :private-land-cd) "Private Land stuff")


(defn match-cde->lookup [lookup cde]
  (take 3 (sort-by #(fuzzy/levenshtein % (name cde))
                   (->> (map :lookup-type-txt lookup-table)
                        (into #{})))))

(into {} (map (juxt identity #(match-cde->lookup lookup-table %)) test-cde))

{:surface-soil-texture-cde ("Fire tolerance" "Protected" "Stream features")
:river-basin-cde ("Parasitic" "Cover Abundance" "Waterbody Type")
:water-body-type-cde ("Waterbody Type" "Activitytype" "Threattype")
:wetland-category-cde ("Origin Category" "Wetland Phase" "SearchType")
:site-shape-cde ("Discipline" "Life Cycle" "Fisheries")
:open-closed-cde ("Howclose" "Specimen" "SearchType")
:geology-cde ("Geology" "Geophyte" "Colour")
:landform-cde ("Landform" "Password" "Land Use")
:wetland-other-class-cde ("Wetland Phase" "Fire tolerance" "Waterbody Type")
:private-land-cde ("Threatened" "Discipline" "Wetland Phase")
:restricted-flag-cde ("Restricted Site" "Restricted Level" "Restricted Access")}

(match-cde->lookup lookup-table (first test-cde))



)

