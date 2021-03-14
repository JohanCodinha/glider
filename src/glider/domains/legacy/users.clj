(ns glider.domains.legacy.users
  (:require [clojure.data.xml :refer [emit-str]]
            [glider.domains.legacy.transaction.users :as users]
            [glider.domains.legacy.transaction.utils :as utils]
            [glider.event-store.core :as es]
            [glider.domains.legacy.auth :as legacy-auth]
            [glider.domains.legacy.transaction.users :as transactions]
            [glider.system :as system]
            [glider.domains.legacy.legacy :as legacy]))

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
  (utils/fetch-rows! users/all-users-transaction 100 cookie))

(defn import-user-by-user-id! [user-id]
  ;;fetch user
  (let [{user-details "UserInfoView_DS"
         organisations "UserOrganisationLink_DS"
         address "AddressDetail_DS"
         contact "ContactDetail_DS"
         :as response}
        (utils/request! (transactions/user-details 757))]
    ;;parse user
    ;;generate events
    response)


  ;;is user aready saved ?
  ;;save/update change
  )

;;Import users from legacy app
(def commands
  [{:id ::import-user!
    :params [:and
             [:map
              [:user-id {:optional true} string?]
              [:username {:optional true} string?]]
             :fn '(fn [{:keys [user-id username]}]
                    (or user-id username))]

    :effect (fn [{:keys [user-id username]}]
              (if user-id
                (import-user-by-user-id! user-id)
                #_(import-user-by-username! username)))
    :return (fn [{:keys [system/effect-return]}]
              effect-return)}])


(comment
  (doseq [event fetched-user] (es/append-to-stream ::user 1 event)))


;fetch users
;if users exist in db
  ;persist change
  ;persist users
