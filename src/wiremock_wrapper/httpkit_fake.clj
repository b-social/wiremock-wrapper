(ns wiremock-wrapper.httpkit-fake
  (:require
    [clojure.string :as string]
    [medley.core :as medley]
    [wiremock-wrapper :as wiremock]))

(defn- normalize-mock-url [url wire-mock-server-atom]
  (string/replace-first url (wiremock/base-url wire-mock-server-atom) ""))

(defn- allEqualTo [headers]
  (into {} (map (fn [[name value]]
                  [name {:equalTo value}])
             headers)))

(defn- relationalize
  ([structure]
   (map (fn [[k v]] (relationalize [k] v)) structure))
  ([path structure]
   (if (map? structure)
     (apply concat (map (fn [[k v]]
                          (relationalize (conj path k) v))
                     structure))
     [path structure])))


(defn- ->matches-json-path [[path value]]
  {:matchesJsonPath {:expression
                     (str "?."
                       (string/join "." (map name path)))
                     :equalTo value}})

(defn- match-paths [body]
  (->>
    body
    relationalize
    (map ->matches-json-path)))

(defn- body->bodyPatterns [request]
  (if
    (contains? request :body)
    (->
      request
      (assoc :bodyPatterns (match-paths (:body request)))
      (dissoc :body))
    request))

(defn- basic-auth->basicAuth [request]
  (if-let [value (get request :basic-auth)]
    (->
      request
      (assoc :basicAuth {:username (first value) :password (second value)})
      (dissoc :basic-auth))
    request))

(defn- oath->headers [request]
  (if-let [value (get request :oauth-token)]
    (->
      request
      (assoc-in [:headers :Authorization] {:equalTo (str "Bearer " value)})
      (dissoc :oauth-token))
    request))

(defn ->wiremock-stub
  "Tries to convert httpkit.fake style stub into wiremock request"
  [wire-mock-server-atom httpkit-fake-stub]
  (let [[request response] httpkit-fake-stub]
    (if (fn? request)
      (throw (ex-info "Unable to transform function matcher"
               {:request request})))
    {:request  (->
                 request
                 (medley/update-existing :url normalize-mock-url wire-mock-server-atom)
                 (medley/update-existing :method #(-> % name string/upper-case))
                 (medley/update-existing :headers allEqualTo)
                 (body->bodyPatterns)
                 (basic-auth->basicAuth)
                 (oath->headers))
     :response response}))

(defn ->wiremock-stubs
  "Tries to convert many httpkit.fake style stubs into wiremock request"
  [wire-mock-server-atom & stubs]
  (map (partial ->wiremock-stub wire-mock-server-atom) stubs))