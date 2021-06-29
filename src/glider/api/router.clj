(ns glider.api.router
  (:require [reitit.ring :as ring]
            [reitit.coercion.malli :as malli-coercion]
            [reitit.ring.malli]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.dev.pretty :as pretty]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.http.interceptors.parameters :as parameters]
            ;[reitit.http.interceptors.dev :as dev]
            [muuntaja.core :as m]
            [malli.util :as mu]
            [reitit.http :as http]
            [reitit.interceptor.sieppari :as sieppari]
            [jsonista.core :as json]
            [glider.api.legacy.user.routes :as legacy-user]
            [glider.api.operation.routes :as operation]))

(defn routes [env]
  [(legacy-user/routes env)
   (operation/routes env)
   ["/debug/:data"
    {:post {:summary "Debug route update"
            #_#_:parameters {:body [:map [:userUid any?]]
                         :query [:map [:all boolean?]]
                         :path [:map [:data any?]]}
            :handler (fn [req]
                       (tap> req)
                       {:status 200
                        :body {:datasource (:db env)
                               #_#_:parameters (:parameters req)}})}}]

   ["/record"
    {:coercion malli-coercion/coercion
     :post {:summary "Upload a record of a species observation to the VBA"
            :tags ["Contribution"]
            :parameters {:body [:map
                                [:latitude int?]
                                [:accuracy int?]
                                [:location-description string?]
                                [:common-name string?]
                                [:scientific-name string?]
                                [:taxon-id string?]
                                [:date-time string?]
                                [:count int?]
                                [:user-id string?]
                                [:notes string?]
                                [:observer-name string?]
                                [:discipine [:enum "fi" "cd" "np"]]]}
            :responses {200 {:description "A record Id is returned on success"
                             :body [:map [:record_id uuid?]]}
                        500 {:description "Server error, record was not save."}}
            :handler (fn [req #_{{{:keys [x y]} :query
                                  {:keys [z]} :path} :parameters}]
                       (let [{{{:keys [x y]} :query
                               {:keys [z]} :path} :parameters} req]
                         (println "made it")
                         (prn x y z)
                         {:status 200, :body {:total ((fnil + 0 0 0) x y z)}}))}}]

   ["/upload"
    {:coercion malli-coercion/coercion

     :post {:summary "Upload a species list CSV file"
            :tags ["Contribution"]
            :description "Accept a CSV file with column matching data model or darwin core field"
            :parameters {:body [:map [:file string?]]}
            :responses {200 {:body [:map [:batch-id uuid?]]}}
            :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                       {:status 200
                        :body {:name (:filename file)
                               :size (:size file)}})}}]
   ["/dashboard"
    {:coercion malli-coercion/coercion
     :get {:summary "System dashboard"
           :tags ["Admin"]
           :description "Features supporting admin to execute command against application state"
           :parameters {:body [:map [:admin-username string?]]}
           :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                      {:status 200
                       :body {:name (:filename file)
                              :size (:size file)}})}}]
   ["/spacial"
    {:coercion malli-coercion/coercion
     :get {:summary "Run spacial queries against live system"
           :tags ["Query"]
           :description "Enabled by PostGIS"
           :parameters {:body [:map
                               [:latitude string?]
                               [:longtude string?]
                               [:species_name string?]
                               [:species_id uuid?]]}
           :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                      {:status 200
                       :body {:name (:filename file)
                              :size (:size file)}})}}]])
(def router-config
  {:data
   {:coercion
    (malli-coercion/create
     {;; set of keys to include in error messages
      :error-keys #{:type :coercion :in :schema :value :errors :humanized :transformed}
           ;; schema identity function (default: close all map schemas)
      :compile mu/closed-schema
           ;; strip-extra-keys (effects only predefined transformers)
      :strip-extra-keys true
           ;; add/set default values
      :default-values true
           ;; malli options
      :options nil})
    :muuntaja m/instance
    :interceptors [;; swagger feature
                   swagger/swagger-feature
                       ;; query-params & form-params
                   (parameters/parameters-interceptor)
                       ;; content-negotiation
                   (muuntaja/format-negotiate-interceptor)
                       ;; encoding response body
                   (muuntaja/format-response-interceptor)
                       ;; exception handling
                   #_(exception/exception-interceptor)
                       ;; decoding request body
                   (muuntaja/format-request-interceptor)
                       ;; coercing response bodys
                   (coercion/coerce-response-interceptor)
                       ;; coercing request parameters
                   (coercion/coerce-request-interceptor)
                       ;; multipart
                   (multipart/multipart-interceptor)]}})
(def swagger-docs
  ["/swagger.json"
   {:get {:no-doc true
          :swagger {:info {:title "Project Glider"
                           :host "google.com"
                           :basePath "/"
                           :contact {:name "Johan Codinha | Biodiversity Foundation Systems Environment and Climate Change"
                                     :email "johan.codinha@delwp.vic.gov.au"}
                           :description "Replacement and upgrade solution for the VBA | Work in progress"}
                    :servers [{:url "https://www.google.com" :description "Production server"}]
                    :tags [{:name "Contribution", :description "Upload observations to the VBA"}
                           {:name "Admin" :description "Administrator dashboard features"}
                           {:name "Query" :description "Location and time base information retrieval"}
                           {:name "Command" :description "Command are task-based data modification that may be asynchronous"}]}
          :handler (swagger/create-swagger-handler)}}])

(defn router [env]
  (http/router
    [swagger-docs
     (routes env)]
    router-config))
(router {})

(defn ring-handler [env]
  (http/ring-handler
   (router env)
   (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))
   {:executor sieppari/executor}))

(comment
  (->
   {:uri "/legacy/synchronization/users/10660"
    :request-method :post}
   ((ring-handler {:db @glider.db/datasource}))
   (update :body (comp json/read-value slurp)))

  (->
   {:uri "/legacy/synchronization/users"
    :request-method :post}
   ((ring-handler {:db @glider.db/datasource}))
   (update :body (comp json/read-value slurp)))

  (->
   {:uri (str "/operation/" "93af550e-7e08-4ee4-80a4-49845c97a776")
    :request-method :get}
   ((ring-handler {:db @glider.db/datasource}))
   #_(update :body (comp json/read-value slurp)))

  "8fe30d94-6f71-4c2d-9368-e91f8fce9ae0"

  (->
   {:uri "/debug"
    :request-method :get
    :parameters {:userUid "10660"}}
   ((ring-handler {:db @glider.db/datasource}))
   (update :body (comp json/read-value slurp))))
