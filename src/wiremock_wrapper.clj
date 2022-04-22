(ns wiremock-wrapper
  (:require
    [org.httpkit.client :as http]
    [freeport.core :refer [get-free-port!]]
    [jason.core :refer [defcoders]]
    [clojure.tools.logging :as log])
  (:import
    [com.github.tomakehurst.wiremock WireMockServer]
    [com.github.tomakehurst.wiremock.core WireMockConfiguration]))


(declare
  ->wire-json
  <-wire-json)

(defcoders wire)

(defn wire-mock-config
  ([] (wire-mock-config (get-free-port!)))
  ([port] (.port (WireMockConfiguration/options) port)))

(defn new-wire-mock-server
  ([] (new-wire-mock-server (wire-mock-config)))
  ([config] (atom (new WireMockServer config))))

(defn base-url [wire-mock-server-port]
  (format "http://localhost:%s"
    wire-mock-server-port))

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

(defn configure-mocks-on [wire-mock-server mocks]
  (doseq [mock mocks]
    (let [mapping (->wire-json mock)]
      @(http/post (mappings-url wire-mock-server)
         {:body mapping}))))

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
                     {:body (->wire-json criteria)})
         response-body (<-wire-json (:body response))
         is-verified (= count (get response-body "count"))]
     (when-not is-verified
       (log/error {:message "request not verified"
                   :wiremock-server-response-body response-body
                   :wiremock-server-response-status (:status response)}))
     is-verified)))

(defn verify-no-unmatched
  [wire-mock-server]
  (let [response @(http/get (unmatched-url wire-mock-server))
        body (<-wire-json (:body response))]
    (when-not (zero? (count (get body "requests")))
      (throw (ex-info "There were unmatched requests" body)))))

(defn respond-with [response]
  (fn [mapping]
    (assoc mapping :response response)))

(defn on-request [request & others]
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

(defmacro with-http-mocks [wire-mock-server-atom mocks & body]
  `(do
     (configure-mocks-on
       (deref ~wire-mock-server-atom)
       (flatten (map #(% {}) ~mocks)))
     ~@body))

(defn with-verify-nounmatched [wire-mock-server-atom]
  (fn [f]
    (f)
    (verify-no-unmatched @wire-mock-server-atom)))

