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
            [ring.adapter.jetty :as jetty]
            [muuntaja.core :as m]
            [clojure.java.io :as io]
            [malli.util :as mu]
            [reitit.http :as http]
            [reitit.interceptor.sieppari :as sieppari]))

(defn interceptor [number]
  {:enter (fn [ctx] (println "intercepted") (update-in ctx [:request :number] (fnil + 0) number))})
#_(defn create-app [db]
  (ring/ring-handler
    (ring/router
      [["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "my-api"
                                :description "with [malli](https://github.com/metosin/malli) and reitit-ring"}
                         :tags [{:name "files", :description "file api"}
                                {:name "math", :description "math api"}]}
               :handler (swagger/create-swagger-handler)}}]

       ["/files"
        {:swagger {:tags ["files"]}}

        ["/upload"
         {:post {:summary "upload a file"
                 :parameters {:multipart [:map [:file reitit.ring.malli/temp-file-part]]}
                 :responses {200 {:body [:map [:name string?] [:size int?]]}}
                 :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                            {:status 200
                             :body {:name (:filename file)
                                    :size (:size file)}})}}]

        ["/download"
         {:get {:summary "downloads a file"
                :swagger {:produces ["image/png"]}
                :handler (fn [_]
                           {:status 200
                            :headers {"Content-Type" "image/png"}
                            :body (-> "reitit.png"
                                      (io/resource)
                                      (io/input-stream))})}}]]

       ["/math"
        {:swagger {:tags ["math"]}}

        ["/plus"
         {:get {:summary "plus with malli query parameters"
                :parameters {:query [:map [:x int?] [:y int?]]}
                :responses {200 {:body [:map [:total int?]]}}
                :handler (fn [{{{:keys [x y]} :query} :parameters}]
                           {:status 200
                            :body {:total (+ x y)}})}
          :post {:summary "plus with malli body parameters"
                 :parameters {:body [:map [:x int?] [:y int?]]}
                 :responses {200 {:body [:map [:total int?]]}}
                 :handler (fn [{{{:keys [x y]} :body} :parameters}]
                            {:status 200
                             :body {:total (+ x y)}})}}]]]

      {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
       ;;:validate spec/validate ;; enable spec validation for route data
       ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
       :exception pretty/exception
       :data {:coercion (reitit.coercion.malli/create
                          {;; set of keys to include in error messages
                           :error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
                           ;; schema identity function (default: close all map schemas)
                           :compile mu/closed-schema
                           ;; strip-extra-keys (effects only predefined transformers)
                           :strip-extra-keys true
                           ;; add/set default values
                           :default-values true
                           ;; malli options
                           :options nil})
              :muuntaja m/instance
              :middleware [;; swagger feature
                           swagger/swagger-feature
                           ;; query-params & form-params
                           parameters/parameters-middleware
                           ;; content-negotiation
                           muuntaja/format-negotiate-middleware
                           ;; encoding response body
                           muuntaja/format-response-middleware
                           ;; exception handling
                           exception/exception-middleware
                           ;; decoding request body
                           muuntaja/format-request-middleware
                           ;; coercing response bodys
                           coercion/coerce-response-middleware
                           ;; coercing request parameters
                           coercion/coerce-request-middleware
                           ;; multipart
                           multipart/multipart-middleware]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))))

(defn create-app [db]
  (http/ring-handler
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
                                {:name "Query" :description "Location and time base information retrieval"}]}
               :handler (swagger/create-swagger-handler)}}]

       ["/record"
        {:coercion malli-coercion/coercion
         :post {:summary "Upload a record of a species observation to the VBA"
                :tags ["Contribution"]
                :parameters {:body [:map
                                    [:latitude int?]
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
                :handler (fn [req #_ {{{:keys [x y]} :query
                                       {:keys [z]} :path} :parameters}]
                           (let [{{{:keys [x y]} :query
                                   {:keys [z]} :path} :parameters} req]
                             (println "made it")
                             (prn x y z)
                             {:status 200, :body {:total ((fnil + 0 0 0) x y z)}}))}}]

       ["/upload"
        {
         :coercion malli-coercion/coercion

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
        {
         :coercion malli-coercion/coercion
         :get {:summary "System dashboard"
                :tags ["Admin"]
                :description "Features supporting admin to execute command against application state"
                :parameters {:body [:map [:admin-username string?]]}
                :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                           {:status 200
                            :body {:name (:filename file)
                                   :size (:size file)}})}}]
       ["/spacial"
        {
         :coercion malli-coercion/coercion
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
                                   :size (:size file)}})}}]
       ]
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
                       (exception/exception-interceptor)
                       ;; decoding request body
                       (muuntaja/format-request-interceptor)
                       ;; coercing response bodys
                       (coercion/coerce-response-interceptor)
                       ;; coercing request parameters
                       (coercion/coerce-request-interceptor)
                       ;; multipart
                       (multipart/multipart-interceptor)]}})
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
  (map (partial ns-unalias *ns*) (keys (ns-aliases *ns*)))
  ((create-app nil) {:request-method :get, :uri "/api/number/99", :query {:number 12}})
  ((create-app nil)  {:request-method :get, :uri "/index.html"})
  )

; {:status 404, :body "", :headers {}}
