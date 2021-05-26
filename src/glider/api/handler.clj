(ns glider.api.handler
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
            [jsonista.core :as json]))

(defn interceptor [number]
  {:enter (fn [ctx] (println "intercepted") (update-in ctx [:request :number] (fnil + 0) number))})

(defn router [db]
  (http/router
   [["/swagger.json"
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
            :handler (swagger/create-swagger-handler)}}]
    
    ["/debug"
     {:get {:summary "Debug route"
            :handler (fn [req]
                       {:status 202
                        :body {:datasource db}}
                       test)}}]

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
                               :size (:size file)}})}}]]
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
                    (multipart/multipart-interceptor)]}}))
(defn create-app [db]
  (http/ring-handler
   (router db)
   ;; sawgger-ui and the default handler
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/create-default-handler))
   ;; executor
   {:executor sieppari/executor}))


(comment
  (def valid-request3
    {:uri "/legacy/syncUser"
     :request-method :post
     :query-params {"a" "1", "EXTRA" "VALUE"}
     :body-params {:b 2, :EXTRA "VALUE"}
     :form-params {:c 3, :EXTRA "VALUE"}
     :headers {"d" "4", "EXTRA" "VALUE"}})
  
  (->
   {:uri "/legacy/syncUser"
    :request-method :post
    :params {"userUid" "10660"}}
   ((create-app {}))
   (update :body (comp json/read-value slurp)))
  
  (map (partial ns-unalias *ns*) (keys (ns-aliases *ns*)))
  ((create-app nil) {:request-method :get, :uri "/api/number/99", :query {:number 12}})
  ((create-app nil)  {:request-method :get, :uri "/index.html"}))
(json/read-value "{\"a\": 1}")
