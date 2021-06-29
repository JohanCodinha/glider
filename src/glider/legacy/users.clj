(ns glider.legacy.users
  (:require [clojure.data.xml :refer [emit-str]]
            #_[glider.event-store.core :as es]
            [glider.legacy.auth :as legacy-auth]
            [glider.legacy.transaction.users :as transactions]
            [glider.legacy.utils :as utils]
            [glider.system.command.core :as command]
            [meander.epsilon :as m]
            #_[glider.system :as system]
            [malli.provider :as mp]
            [malli.core :as malli]
            [malli.transform :as mt]
            [malli.error :as me]
            [glider.domain.collaborator.collaborator :refer [collaborator]]
            [glider.domain.collaborator.contact-method :refer [Schema]]
            [glider.domain.collaborator.address :as address]
            [malli.util :as mu]
            [java-time :as time]
            [editscript.core :as diff]
            [lib.editscript.core :as editscript]
            [glider.utils :refer [uuid timestamp]]
            [glider.db :refer [select! insert! execute!]]
            [crypto.password.bcrypt :as crypto]
            [clojure.core.async :as cca]
            [glider.system.operation.core :as operation]
            [clojure.walk :refer [postwalk]]))

;; Aggregate
;; Handling users management sync with legacy system.
(defn get-all-users!
  "Fetch all users, return a lazy seq"
  [cookie]
  (println "get all users")
  (-> (utils/request2! (transactions/all-users-transaction) cookie)
      first
      :data))


(comment
  (transactions/all-users-transaction)
  (def all-users (future (get-all-users! @legacy-auth/admin-cookie)))

  (def all-userUid
    (map #(str (get % "userUid")) (-> @all-users first :data)))

  )

(defn fetch-user-details! [user-id cookie]
  (utils/request2! (transactions/user-details user-id) cookie))


(defn import-user-by-userUid! [user-id cookie]
  (let [res (fetch-user-details! user-id cookie)]
    res))

(def refresh-cookie
  [:cookie
   (fn [ctx] (legacy-auth/refresh-cookie))])

(def legacy-cookie
  [:cookie
   (fn [ctx]
     (let [previous-request (count (filter #{:cookie} (:execute/stack ctx)))]
       (case previous-request
         0 @legacy-auth/admin-cookie
         1 (legacy-auth/refresh-cookie)
         (throw (ex-info "Authentication loop" ctx)))))])

(defn import-user
  [{{cookie :cookie} :cofx
    {:keys [userUid username]} :params :as ctx}]
  (try
    (fetch-user-details! userUid cookie)
    (catch clojure.lang.ExceptionInfo e
      (if (= :cookie-expired (:type (ex-data e)))
        (command/enqueue ctx legacy-cookie)
        (throw e)))))

(defn import-users-list!
  [{{cookie :cookie} :cofx
    :as ctx}]
  (try
    (get-all-users! cookie)
    (catch clojure.lang.ExceptionInfo e
      (if (= :cookie-expired (:type (ex-data e)))
        (doto (command/enqueue ctx legacy-cookie) tap>)
        (throw e)))))

(defn legacy-user-synchronized-event
  [data userUid version]
  {:id (uuid)
   :type :legacy-user-synchronized
   :stream-id userUid
   :created-at (timestamp)
   :version version
   :payload data})




(defn diff-payload [saved imported]
  (try
    (let [saved-by-operation (into {} (map (juxt :operation identity) saved))]
      (if saved
        (->> imported
             (map (fn [{operation :operation :as i}]
                    (update i :data #(editscript/diff->edits
                                      (:data (get saved-by-operation
                                                  operation))
                                      %))))
             (remove #(empty? (:data %)))
             vec)
        imported))
    (catch Exception e
      (println "diff failled, why ?")
      (tap> saved)
      (tap> imported)
      (throw e))))


(comment
  (diff-payload [{:operation {:content [{:content [{:content ["34"]
                                         :attrs nil
                                         :tag :userUid}]
                              :attrs {:xsi:type "xsd:Object"}
                              :tag :criteria}
                             {:content [{:content ["UserInfoView_DS"]
                                         :attrs nil
                                         :tag :dataSource}
                                        {:content ["fetch"]
                                         :attrs nil
                                         :tag :operationType}]
                              :attrs {:xsi:type "xsd:Object"}
                              :tag :operationConfig}
                             {:content ["builtinApplication"]
                              :attrs nil
                              :tag :appID}
                             {:content ["userInfoById"]
                              :attrs nil
                              :tag :operation}]
                   :attrs {:xsi:type "xsd:Object"}
                   :tag :elem}
       :data-source "UserInfoView_DS"
       :data [{"confirmUserPasswordTxt" nil
               "creationTsp" 1313416800000
               "preferredContactMethodCde" "pcme"
               "statusCde" "active"
               "nameId" 34
               "organisationId" 99
               "modifiedTsp" 1469664556170
               "changePasswordCde" "false"
               "fullName" "Dale Smithyman"
               "statusDesc" "Active"
               "organisationNme" "Golden Plains Shire Council"
               "roleDesc" "Contributor"
               "primaryAddressId" 1
               "roleCde" "con"
               "primaryContactId" 3445
               "loginNameNme" "Smithyman"
               "surnameNme" "Smithyman"
               "otherNme" nil
               "batchUploadViewCde" "false"
               "givenNme" "Dale"
               "lastSystemAccessTsp" 1469664556138
               "userUid" 34
               "reasonTxt" "LGA user"
               "restrictedViewingCde" "false"
               "dateAcceptedTcTsp" nil}]}
      {:operation {:content [{:content [{:content ["34"]
                                         :attrs {:xsi:type "xsd:long"}
                                         :tag :USER_UID}]
                              :attrs {:xsi:type "xsd:Object"}
                              :tag :criteria}
                             {:content [{:content ["AddressDetail_DS"]
                                         :attrs nil
                                         :tag :dataSource}
                                        {:content ["fetch"]
                                         :attrs nil
                                         :tag :operationType}
                                        {:content ["exact"]
                                         :attrs nil
                                         :tag :textMatchStyle}]
                              :attrs {:xsi:type "xsd:Object"}
                              :tag :operationConfig}
                             {:content ["isc_ListGrid_1"]
                              :attrs nil
                              :tag :componentId}
                             {:content ["builtinApplication"]
                              :attrs nil
                              :tag :appID}
                             {:content ["fetchUserAddresses"]
                              :attrs nil
                              :tag :operation}]
                   :attrs {:xsi:type "xsd:Object"}
                   :tag :elem}
       :data-source "AddressDetail_DS"
       :data [{"addressId" 1
               "creationTsp" 1313416800000
               "mainAddressId" ""
               "modifiedTsp" nil
               "streetNme" nil
               "countryNme" nil
               "postcodeTxt" nil
               "cityNme" nil
               "streetNumberTxt" nil
               "stateNme" nil}
              {"addressId" 1832
               "creationTsp" 1469084015579
               "mainAddressId" ""
               "modifiedTsp" nil
               "streetNme" "P. O. Box 111"
               "countryNme" "Australia"
               "postcodeTxt" "3328"
               "cityNme" "Bannockburn"
               "streetNumberTxt" nil
               "stateNme" "VIC"}]}
      {:operation {:content [{:content [{:content ["34"]
                                         :attrs {:xsi:type "xsd:long"}
                                         :tag :USER_UID}]
                              :attrs {:xsi:type "xsd:Object"}
                              :tag :criteria}
                             {:content [{:content ["ContactDetail_DS"]
                                         :attrs nil
                                         :tag :dataSource}
                                        {:content ["fetch"]
                                         :attrs nil
                                         :tag :operationType}
                                        {:content ["exact"]
                                         :attrs nil
                                         :tag :textMatchStyle}]
                              :attrs {:xsi:type "xsd:Object"}
                              :tag :operationConfig}
                             {:content ["isc_ListGrid_2"]
                              :attrs nil
                              :tag :componentId}
                             {:content ["builtinApplication"]
                              :attrs nil
                              :tag :appID}
                             {:content ["fetchContactByUserUid"]
                              :attrs nil
                              :tag :operation}]
                   :attrs {:xsi:type "xsd:Object"}
                   :tag :elem}
       :data-source "ContactDetail_DS"
       :data [{"creationTsp" nil
               "contactCde" nil
               "emailOrPhoneTxt" nil
               "modifiedTsp" nil
               "contactId" 1}
              {"creationTsp" 1469069246595
               "contactCde" "cde"
               "emailOrPhoneTxt" "dsmithyman@gplains.vic.gov.au"
               "modifiedTsp" nil
               "contactId" 3445}]}]

                [{:data [{"confirmUserPasswordTxt" nil
               "creationTsp" 1313416800000
               "preferredContactMethodCde" "pcme"
               "statusCde" "active"
               "nameId" 34
               "organisationId" 99
               "modifiedTsp" 1469664556170
               "changePasswordCde" "false"
               "fullName" "Dale Smithyman"
               "statusDesc" "Active"
               "organisationNme" "Golden Plains Shire Council"
               "roleDesc" "Contributor"
               "primaryAddressId" 1
               "roleCde" "con"
               "primaryContactId" 3445
               "loginNameNme" "Smithyman"
               "surnameNme" "Smithyman"
               "otherNme" nil
               "batchUploadViewCde" "false"
               "givenNme" "Dale"
               "lastSystemAccessTsp" 1469664556138
               "userUid" 34
               "reasonTxt" "LGA user"
               "restrictedViewingCde" "false"
               "dateAcceptedTcTsp" nil}]
       :operation {:tag :elem
                   :attrs {:xsi:type "xsd:Object"}
                   :content [{:tag :criteria
                              :attrs {:xsi:type "xsd:Object"}
                              :content [{:tag :userUid
                                         :attrs nil
                                         :content ["34"]}]}
                             {:tag :operationConfig
                              :attrs {:xsi:type "xsd:Object"}
                              :content [{:tag :dataSource
                                         :attrs nil
                                         :content ["UserInfoView_DS"]}
                                        {:tag :operationType
                                         :attrs nil
                                         :content ["fetch"]}]}
                             {:tag :appID
                              :attrs nil
                              :content ["builtinApplication"]}
                             {:tag :operation
                              :attrs nil
                              :content ["userInfoById"]}]}}
      {:data [{"addressId" 1832
               "creationTsp" 1469084015579
               "mainAddressId" ""
               "modifiedTsp" nil
               "streetNme" "P. O. Box 111"
               "countryNme" "Australia"
               "postcodeTxt" "3328"
               "cityNme" "Bannockburn"
               "streetNumberTxt" nil
               "stateNme" "VIC"}
              {"addressId" 1
               "creationTsp" 1313416800000
               "mainAddressId" ""
               "modifiedTsp" nil
               "streetNme" nil
               "countryNme" nil
               "postcodeTxt" nil
               "cityNme" nil
               "streetNumberTxt" nil
               "stateNme" nil}]
       :operation {:tag :elem
                   :attrs {:xsi:type "xsd:Object"}
                   :content [{:tag :criteria
                              :attrs {:xsi:type "xsd:Object"}
                              :content [{:tag :USER_UID
                                         :attrs {:xsi:type "xsd:long"}
                                         :content ["34"]}]}
                             {:tag :operationConfig
                              :attrs {:xsi:type "xsd:Object"}
                              :content [{:tag :dataSource
                                         :attrs nil
                                         :content ["AddressDetail_DS"]}
                                        {:tag :operationType
                                         :attrs nil
                                         :content ["fetch"]}
                                        {:tag :textMatchStyle
                                         :attrs nil
                                         :content ["exact"]}]}
                             {:tag :componentId
                              :attrs nil
                              :content ["isc_ListGrid_1"]}
                             {:tag :appID
                              :attrs nil
                              :content ["builtinApplication"]}
                             {:tag :operation
                              :attrs nil
                              :content ["fetchUserAddresses"]}]}}
      {:data [{"contactCde" nil
               "contactId" 1
               "creationTsp" nil
               "emailOrPhoneTxt" nil
               "modifiedTsp" nil}
              {"contactCde" "cde"
               "contactId" 3445
               "creationTsp" 1469069246595
               "emailOrPhoneTxt" "dsmithyman@gplains.vic.gov.au"
               "modifiedTsp" nil}]
       :operation {:tag :elem
                   :attrs {:xsi:type "xsd:Object"}
                   :content [{:tag :criteria
                              :attrs {:xsi:type "xsd:Object"}
                              :content [{:tag :USER_UID
                                         :attrs {:xsi:type "xsd:long"}
                                         :content ["34"]}]}
                             {:tag :operationConfig
                              :attrs {:xsi:type "xsd:Object"}
                              :content [{:tag :dataSource
                                         :attrs nil
                                         :content ["ContactDetail_DS"]}
                                        {:tag :operationType
                                         :attrs nil
                                         :content ["fetch"]}
                                        {:tag :textMatchStyle
                                         :attrs nil
                                         :content ["exact"]}]}
                             {:tag :componentId
                              :attrs nil
                              :content ["isc_ListGrid_2"]}
                             {:tag :appID
                              :attrs nil
                              :content ["builtinApplication"]}
                             {:tag :operation
                              :attrs nil
                              :content ["fetchContactByUserUid"]}]}}]))
;;Import users from legacy app
(defn fetch-saved-user-stream [userUid]
  (select! ["SELECT * FROM legacy_events WHERE stream_id = ? ORDER BY version" userUid]))

(defn merge-diffs
  "Merge a stream of diff, return nil if passed empty vector"
  [stream]
  (reduce
   (fn
     ([])
     ([a b]
      (mapv (fn [{operation :operation :as payload}]
              (let [data-b (:data (first (get (group-by :operation b) operation)))]
                (cond-> payload
                  (and (some? data-b) (diff/valid-edits? data-b))
                  (update :data #(diff/patch % (diff/edits->script data-b))))))
            a)))
   (map :payload stream)))

(defn extract-credentials [user-data]
  (m/find
   user-data
   (m/scan {:data [{"userPasswordTxt" ?password
                    "loginNameNme" ?login}]})
   {:password ?password
    :login ?login}))

(defn remove-key [user-data key]
  (clojure.walk/postwalk
   #(when-not (and (vector? %) (= (first %) key))
      %)
   user-data))

(defn fetch-saved-user-password [userUid]
  (first (select! ["SELECT * FROM authentication WHERE legacy_uid = ?" userUid])))


(defn fetch-credentials
  [{:keys [login uuid legacy-uid]}]
  (first
   (select!
    ["SELECT * FROM authentication WHERE uuid = ? OR login = ? OR legacy_uid = ?"
     uuid login legacy-uid])))

(defn upsert-authentication-credentials!
  [{:keys [uuid login legacy-uid] :as credentials} {db :db}]
  (let [exist (fetch-credentials credentials)]
    (when exist
      (execute! db ["DELETE FROM authentication WHERE uuid = ? OR login = ? OR legacy_uid = ?"
                    uuid login legacy-uid]))
    (insert! db :authentication
             credentials)))

(defn valid-credentials?
  [{:keys [password login uuid] :as credentials}]
  (let [{encrypted-passwrod :authentication/password}
        (fetch-credentials credentials)]
    (crypto/check password encrypted-passwrod)))


(defn password-changed [saved new]
  (cond
    (and (nil? saved)
         (string? new))
    new
    (and (string? saved)
         (string? new)
         (not (crypto/check new saved)))
    new))
(defn user-exists? [imported-user]
  (not-every? (fn [{data :data}]
                (or (= data nil) (= data [])))
              imported-user))

(def import-user-command
  {:id ::import-user
   :params [:map
            [:userUid :string]]
   :coeffects [legacy-cookie
               [:saved-user-stream
                (fn [{{:keys [userUid]} :params}]
                  (fetch-saved-user-stream userUid))]
               [:saved-user-password
                (fn [{{:keys [userUid]} :params}]
                  (:password (fetch-saved-user-password userUid)))]
               [:imported-user import-user]]

   :conditions [[(fn [{{:keys [imported-user]} :cofx}]
                   (user-exists? imported-user))
                 :not-found "User with this id does not exist"]]

   :effects (fn [{{:keys [saved-user-stream imported-user saved-user-password]} :cofx
                  {:keys [userUid]} :params}]
              (println userUid)
              (tap> saved-user-stream)
              (tap> imported-user)
              (tap> saved-user-password)
              (let [{password :password
                     login :login} (extract-credentials imported-user)
                    saved-user (merge-diffs saved-user-stream)
                    next-version (-> saved-user-stream
                                     last
                                     (get :version 0)
                                     inc)
                    data-diff (diff-payload saved-user
                                            (mapv
                                             #(select-keys % [:data :operation])
                                             (remove-key imported-user "userPasswordTxt")))
                    new-password (password-changed saved-user-password password)]
                {:save-legacy-event
                 (when-not (empty? data-diff)
                   {:id (uuid)
                    :type :legacy-user-synchronized
                    :stream-id userUid
                    :created-at (timestamp)
                    :version next-version
                    :payload data-diff})
                 :save-credentials
                 (when new-password
                   {:login login
                    :legacy-uid userUid
                    :password (crypto/encrypt new-password)})}))
   :handler {:save-legacy-event (fn [payload environment]
                                  (insert! (:db environment) :legacy-events payload))
             :save-credentials upsert-authentication-credentials!}
   :return #(select-keys % [:side-effects])
   :produce [:map
             [:side-effects
              [:map
               [:save-legacy-event [:or map? nil?]
                :save-credentials [:or map? nil?]]]]]})

(defn fetch-users
  [{{:keys [cookie users-list]} :cofx :as ctx}]
  (reduce (fn [acc id]
            (let [user (fetch-user-details! id cookie)]
              (operation/update!
               (get-in ctx [:environment :db])
               (update-in (:operation ctx) [:metadata :users-fetched] (fnil inc 0)))
              (conj acc user)))
          [] (map #(get % "userUid") users-list)))

(def import-users-command
  {:id ::import-users
   :coeffects [legacy-cookie
               [:users-list import-users-list!]
               [:update-operation
                (fn [{{:keys [users-list]} :cofx :as ctx}]
                  (operation/update!
                   (get-in ctx [:environment :db])
                   (assoc-in (:operation ctx) [:metadata :users-to-fetch] (count users-list))))]
               [:fetch-users fetch-users]]
   :return identity})

(comment (command/run! import-users-command)
         )

(comment
  (def ev (command/run! import-user-command
                        {:userUid "16"}
                        {:environment {:db @glider.db/datasource}}
                        #_{:side-effects false}))

  (def iuser (import-user-by-userUid! "10660" @legacy-auth/admin-cookie))

  (legacy-auth/refresh-cookie)

  (execute! ["DELETE FROM legacy_events WHERE stream_id = ?" "10660"])
  (def re (first (select! ["SELECT * FROM legacy_events WHERE stream_id = ?" "10660"])))

  ;; Test that merging all update bring aggregate to same state as current.
  (= (mapv #(dissoc % :operation) (:res iuser))
     (merge-diffs (fetch-saved-user-stream "10660")))

  (-> "10660"
      fetch-saved-user-stream
      merge-diffs)

  (count (fetch-saved-user-stream "10660"))

  (def lookups-data
    (utils/request! {:tag :transaction
                     :attrsiy
                     {:xsi:type "xsd:Object",
                      :xmlns:xsi "http://www.w3.org/2000/10/XMLSchema-instance"},
                     :content
                     [{:tag :operations,
                       :attrs {:xsi:type "xsd:List"},
                       :content
                       [{:tag :elem,
                         :attrs {:xsi:type "xsd:Object"},
                         :content
                         [{:tag :criteria, :attrs {:xsi:type "xsd:Object"}, :content nil}
                          {:tag :operationConfig,
                           :attrs {:xsi:type "xsd:Object"},
                           :content
                           [{:tag :dataSource,
                             :attrs nil,
                             :content ["LookupExpansion_DS"]}
                            {:tag :operationType, :attrs nil, :content ["fetch"]}
                            {:tag :textMatchStyle, :attrs nil, :content ["substring"]}]}
                          {:tag :componentId,
                           :attrs nil,
                           :content ["isc_ReferenceDataModule$1_0"]}
                          {:tag :appID, :attrs nil, :content ["builtinApplication"]}
                          {:tag :operation,
                           :attrs nil,
                           :content ["LookupExpansion_DS_fetch"]}]}]}]}
                    @legacy-auth/admin-cookie))

  (defn index-lookups [lookups]
    (into {}
          (map (fn [[type ls]]
                 [type (into {} (map (fn [l]
                                       [(get l "lookupCde") (get l "lookupDesc")]) ls))])
               (group-by #(get % "lookupTypeTxt")
                         lookups))))

  (def lookups
    (index-lookups (get lookups-data "LookupExpansion_DS"))))


   
(defn hydrate-contact [lookups user-raw-response]
  (m/search
   (assoc user-raw-response "lookups" lookups)
   {"lookups" {"Contact Detail" {?contactCde ?desc}}
    "UserInfoView_DS" [{"primaryContactId" ?primaryContactId}]
    "ContactDetail_DS" (m/scan {"contactCde" ?contactCde
                                "contactId" ?contactId
                                "emailOrPhoneTxt" ?emailOrPhoneTxt
                                & ?rest})}
   (merge {:type ?desc
           :primary (= ?primaryContactId ?contactId)}
          ?rest
          (if (= ?desc "Email Address")
            {:address ?emailOrPhoneTxt}
            {:number  ?emailOrPhoneTxt}))))

(defn hydrate-addresses [user-raw-response]
  (m/search
   user-raw-response
   {"UserInfoView_DS" [{"primaryAddressId" ?primaryAddressId}]
    "AddressDetail_DS" (m/scan {"addressId" ?addressId & ?rest})}
    (conj {:primary (= ?primaryAddressId ?addressId)}
          ?rest)))

(comment
  (hydrate-contact lookups iuser)
  (hydrate-addresses iuser))

(defn remove-nils-and-string-keys
  [m]
  (into {} (remove (fn [[k v]] (or (string? k) (nil? v))) m)))

(def keys-mapping
  {"UserInfoView_DS"
   {"creationTsp" :account-creation-date,
    "statusDesc" :status,
    "roleDesc" :role,
    "loginNameNme" :login-name,
    "surnameNme" :surname,
    "otherNme" :other-name,
    "batchUploadViewCde" :batch-upload-access,
    "givenNme" :given-name,
    "userUid" :legacy-Uid,
    "reasonTxt" :reason-of-use,
    "restrictedViewingCde" :restricted-viewing-access,
    "dateAcceptedTcTsp" :terms-and-conditions-accepted-date}
   "ContactDetail_DS"
   {"contactCde" :type}
   "AddressDetail_DS"
   {"creationTsp" :supplied-date,
    "streetNme" :street-name,
    "countryNme" :country-name,
    "postcodeTxt" :postcode,
    "cityNme" :city,
    "streetNumberTxt" :street-number,
    "stateNme" :state}})


(comment
  (def contact
    (->> iuser
         (hydrate-contact lookups)
         (map  #(clojure.set/rename-keys % (get keys-mapping "ContactDetail_DS")))
         (map remove-nils-and-string-keys)))

  (def addresses
    (->> iuser
         hydrate-addresses
         (map  #(clojure.set/rename-keys % (get keys-mapping "AddressDetail_DS")))
         (map remove-nils-and-string-keys)))

  (def saved
    (-> iuser
        (get-in #_iuser ["UserInfoView_DS" 0])
        #_(hydrate-user)
        (clojure.set/rename-keys (get keys-mapping "UserInfoView_DS"))
        remove-nils-and-string-keys))
  saved

  (map #(malli/validate Schema %) contact)
  (map #(if (malli/validate address/Schema %)
          %
          (me/humanize (malli/explain address/Schema %))) addresses)
  iuser

  (map #(address/parse %) addresses)
  (get-in {"a" [1 2]} ["a" 0])

  (collaborator saved))

;store raw data
