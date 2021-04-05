(ns glider.legacy.users
  (:require [clojure.data.xml :refer [emit-str]]
            [glider.event-store.core :as es]
            [glider.legacy.auth :as legacy-auth]
            [glider.legacy.transaction.users :as transactions]
            [glider.legacy.utils :as utils]
            [glider.system.command :as command]
            [meander.epsilon :as m]
            [glider.system :as system]
            [malli.provider :as mp]
            [malli.core :as malli]
            [malli.transform :as mt]
            [malli.error :as me]
            [glider.domain.collaborator.collaborator :refer [collaborator]]
            [glider.domain.collaborator.contact-method :refer [Schema]]
            [glider.domain.collaborator.address :as address]
            [malli.util :as mu]
            [java-time :as time]
            [editscript.core :as diff]))

;; Aggregate
;; Handling users management sync with legacy system.

#_(defn vba-user->user-retrieved-event
  "map an imported vba user to a saved event"
  [raw-json-user]
  (es/payload->events {:type ::retrieved-user
                       :payload raw-json-user}))

#_(defn fetch-user->save-as-event!
  [cookie]
  (transduce 
    (comp 
      (map #(do (utils/fetched-rows-report %)
                %))
      (mapcat #(get % "data"))
      (map vba-user->user-retrieved-event))
    conj []
    (users/get-all-users! cookie)))

(defn get-all-users!
  "Fetch all users, return a lazy seq"
  [cookie]
  (utils/fetch-rows! transactions/all-users-transaction 100 cookie))

(defn fetch-user-details! [user-id cookie]
  (utils/request! (transactions/user-details user-id) cookie))

(def u10660 (fetch-user-details! "10660" @legacy-auth/admin-cookie))



(defn import-user-by-userUid! [user-id cookie]
  (let [res (fetch-user-details! user-id cookie)]
    res))

(transactions/user-details 757)

(def refresh-cookie
  [:cookie
   (fn [& _] (legacy-auth/refresh-cookie))])

;;Import users from legacy app
(def commands
  [{:id ::import-user!
    :params [:and
             [:map
              [:userUid #_{:optional true}
               [:string]]
              [:username {:optional true}
               [:re {:error/message "User name can contain letters, numbers and underscores. The length must be between 6 and 15 characters."}
                #"^[a-zA-Z_\d]{6,}$"]]]
             [:fn
              {:error/path [:userUid]
               :error/message "missing required key"}
              '(fn [{:keys [userUid username]}]
                 (or userUid username))]]
    :coeffects [[:cookie
                 (fn [& _]
                  @legacy-auth/admin-cookie)]
                [:saved-user
                 (fn [{{:keys [userUid username]} :params}]
                   nil #_u10660)]
                [:imported-user
                 (fn [{{cookie :cookie} :cofx
                       {:keys [userUid username]} :params :as ctx}]
                   (let [{user :res :as res} (fetch-user-details! userUid cookie)]
                     (tap> user)
                     (cond (and (= (:system/error res) :login-required)
                              (>= 1 (count (filter #{:cookie} (:execute/stack ctx)))))
                           (command/enqueue ctx refresh-cookie)
                           (and (= (:system/error res) :login-required)
                                (> 1 (count (filter #{:cookie} (:execute/stack ctx)))))
                           (throw (ex-info "Authentication loop" ctx))
                           :else user)))]]
    :effect (fn [{{:keys [saved-user imported-user]} :cofx
                  {:keys [userUid username]} :params}]
              (let [saved-user-by-data-source (into {} (map (juxt :data-source identity) saved-user))]
                (if saved-user
                  (->> imported-user
                       (map (fn [{data-source :data-source :as i}]
                              (-> i
                                  (assoc :diff (diff/get-edits
                                                (diff/diff (:data (get saved-user-by-data-source
                                                                       data-source))
                                                           (:data i))))
                                  (dissoc :data))))
                       (remove #(empty? (:diff %))))
                  imported-user)))

    :return (fn [{effect-return :effect}]
              effect-return)
    :produce [:legacy-user-imported :legacy-user-updated]}])


(comment (diff/get-edits (diff/diff [{:a {:c 3}}] [{:a 1}]))
         (diff/patch {} (diff/diff nil {:b 3}))
         u10660)

(comment
  (def u957 (import-user-by-userUid! 957 @legacy-auth/admin-cookie))
  (map command/register-command commands)
  (command/run! ::import-user! {:userUid "10660" :username "codeforvic"})
  
  (def iuser (import-user-by-userUid! 757 @legacy-auth/admin-cookie))

  (def lookups-data
    (utils/request! {:tag :transaction
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
    (into {} (map (fn [[type ls]]
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

  (def saved-user
    (-> iuser
        (get-in #_iuser ["UserInfoView_DS" 0])
        #_(hydrate-user)
        (clojure.set/rename-keys (get keys-mapping "UserInfoView_DS"))
        remove-nils-and-string-keys))
  saved-user

  (map #(malli/validate Schema %) contact)
  (map #(if (malli/validate address/Schema %)
          %
          (me/humanize (malli/explain address/Schema %))) addresses)
  iuser

  (map #(address/parse %) addresses)
  (get-in {"a" [1 2]} ["a" 0])

  (collaborator saved-user))

;store raw data
