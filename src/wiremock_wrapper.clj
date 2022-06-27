(ns wiremock-wrapper
  (:require
    [org.httpkit.client :as http]
    [freeport.core :refer [get-free-port!]]
    [jason.core :refer [defcoders]])
  (:import
    [com.github.tomakehurst.wiremock WireMockServer]
    [com.github.tomakehurst.wiremock.core WireMockConfiguration]))

(def COMMON-HEADERS {"Content-Type" {:equalTo "application/json"}
                     "Accept" {:equalTo "application/hal+json"}})

(def ADMIN-AUTH-HEADERS
  (merge COMMON-HEADERS
         {"Authorization" {:equalTo "ADMIN"}}))

(declare
  ->wire-json
  <-wire-json)

(defcoders wire)

(defn new-wire-mock-server
  []
  (->> (get-free-port!)
       (.port (WireMockConfiguration/options))
       (new WireMockServer)
       (atom)))

(defn base-url
  [wire-mock-server-atom]
  (str "http://localhost:"
       (.portNumber (.getOptions @wire-mock-server-atom))))

(defn service-mock-base-url
  [wire-mock-server-atom service-name]
  (str (base-url wire-mock-server-atom)
       "/"
       (name service-name)))

(defn url [wire-mock-server path]
  (.url wire-mock-server path))

(defn reset-url [wire-mock-server]
  (url wire-mock-server "/__admin/reset"))

(defn mappings-url [wire-mock-server]
  (url wire-mock-server "/__admin/mappings"))

(defn requests-url [wire-mock-server]
  (url wire-mock-server "/__admin/requests"))

(defn counts-url [wire-mock-server]
  (url wire-mock-server "/__admin/requests/count"))

(defn unmatched-url [wire-mock-server]
  (url wire-mock-server "/__admin/requests/unmatched"))

(defn start [^WireMockServer wire-mock-server]
  (.start wire-mock-server))

(defn stop [^WireMockServer wire-mock-server]
  (.stop wire-mock-server))

(defn reset [wire-mock-server]
  @(http/post (reset-url wire-mock-server)))

(defn configure-mocks-on
  [wire-mock-server-atom mocks]
  (doseq [mock mocks]
    (let [response (-> (mappings-url @wire-mock-server-atom)
                       (http/post {:body (->wire-json mock)})
                       (deref))]
      (when-not (= (:status response) 201)
        (->> response
             (ex-info "Error while adding mapping to WireMock server")
             (throw))))))

(defn get-mappings-from [wire-mock-server]
  (let [response @(http/get (mappings-url wire-mock-server))
        mappings (<-wire-json (:body response))]
    mappings))

(defn get-requests-from [wire-mock-server]
  (let [response @(http/get (requests-url wire-mock-server))
        requests (<-wire-json (:body response))]
    requests))

(defn verify
  ([wire-mock-server criteria]
   (verify wire-mock-server criteria 1))
  ([wire-mock-server criteria count]
   (let [response @(http/post (counts-url wire-mock-server)
                     {:body (->wire-json criteria)})]
     (= count (get (<-wire-json (:body response)) "count")))))

(defn verify-no-unmatched
  [wire-mock-server]
  (let [response @(http/get (unmatched-url wire-mock-server))
        body (<-wire-json (:body response))]
    (when-not (zero? (count (get body "requests")))
      (throw (ex-info "There were unmatched requests" body)))))

(defn ^:deprecated respond-with
  [response]
  (fn [mapping]
    (assoc mapping :response response)))

(defn ^:deprecated on-request
  [request & others]
  (fn [mapping]
    (reduce
      (fn [m o] (o m))
      (assoc mapping :request request)
      others)))

(defn in-scenario [scenario]
  (fn [mapping]
    (assoc mapping :scenarioName scenario)))

(defn in-state [current-state]
  (fn [mapping]
    (assoc mapping :requiredScenarioState current-state)))

(defn move-to-state [new-state]
  (fn [mapping]
    (assoc mapping :newScenarioState new-state)))

(defn with-empty-wire-mock-server [wire-mock-server-atom]
  (fn [f]
    (reset @wire-mock-server-atom)
    (f)))

(defn with-wire-mock-server [wire-mock-server-atom]
  (fn [f]
    (try
      (start @wire-mock-server-atom)
      (f)
      (finally
        (stop @wire-mock-server-atom)))))

;; Don't use the macro, use configure-mocks-on directly
;; Macro doesn't create a new scope where mocks are registered on server
;; They will be there even outside the macro, so it's just adding a new nested level
(defmacro ^:deprecated with-http-mocks
  [wire-mock-server-atom mocks & body]
  `(do
     (configure-mocks-on
       ~wire-mock-server-atom
       (flatten (map #(% {}) ~mocks)))
     ~@body))

(defn with-verify-nounmatched [wire-mock-server-atom]
  (fn [f]
    (f)
    (verify-no-unmatched @wire-mock-server-atom)))

