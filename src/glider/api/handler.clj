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

(defn create-app [db]
  (http/ring-handler
    (http/router
      [["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "Project Glider"
                                :description "http ring malli project glider"}
                         :tags [{:name "math", :description "math api"}]}
               :handler (swagger/create-swagger-handler)}}]
       ["/record"
        {:coercion malli-coercion/coercion
         :post {:summary "Upload a record of a species observation to the VBA"
               ;:parameters {:query [:map [:cmd string?] [:data map?]]}
               ;:responses {200 {:body [:map [:res string?]]}}
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
               :swagger {:responses {200 {:schema {:type "string"}
                                          :description "record id"}}}
               :responses {200 {:body [:map [:number int?]]}
                           500 {:description "fail"}}
               :handler (fn [req #_ {{{:keys [x y]} :query
                                      {:keys [z]} :path} :parameters}]
                          (let [{{{:keys [x y]} :query
                                  {:keys [z]} :path} :parameters} req]
                            (println "made it")
                            (prn x y z)
                            {:status 200, :body {:total ((fnil + 0 0 0) x y z)}}))}}]
       ["/api"
        {:interceptors [(interceptor 1)]}

        ["/number/*z"
         {:interceptors [(interceptor 10)]
          :coercion malli-coercion/coercion
          :get {:interceptors [(interceptor 100)]
                :summary "add numbers"
                ;:parameters {:query [:map [:cmd string?] [:data map?]]}
                ;:responses {200 {:body [:map [:res string?]]}}
                :parameters {:body [:maybe [:vector int?]]
                             :path [:map [:z int?]]}
                :swagger {:responses {400 {:schema {:type "string"}
                                           :description "some num"}}}
                :responses {200 {:body [:map [:number int?]]}
                                  500 {:description "fail"}}
                :handler (fn [req #_ {{{:keys [x y]} :query
                                       {:keys [z]} :path} :parameters}]
                           (let [{{{:keys [x y]} :query
                                   {:keys [z]} :path} :parameters} req]
                             (println "made it")
                             (prn x y z)
                             {:status 200, :body {:total ((fnil + 0 0 0) x y z)}}))}}]]]
      {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
       ;;:validate spec/validate ;; enable spec validation for route data
       ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
       ;:exception pretty/exception
       :data {:coercion (malli-coercion/create
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
                             (multipart/multipart-interceptor)]
              }})
    ;; the default handler
    ;(ring/create-default-handler)
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
  ((create-app nil) {:request-method :get, :uri "/swagger.json"}))
; {:status 404, :body "", :headers {}}
