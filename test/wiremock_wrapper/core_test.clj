(ns wiremock-wrapper.core-test
  (:require [clojure.test :refer :all]
            [freeport.core :refer [get-free-port!]]
            [org.httpkit.client :as http-kit]
            [wiremock-wrapper :refer :all]
            [jason.convenience :as jason]))


(let [wire-mock-server-port (get-free-port!)
      wire-mock-config (wire-mock-config wire-mock-server-port)
      wire-mock-server (new-wire-mock-server wire-mock-config)
      wire-mock-address (base-url wire-mock-server-port)]
  (use-fixtures :once
    (with-wire-mock-server wire-mock-server))
  (use-fixtures :each
    (with-empty-wire-mock-server wire-mock-server)
    (with-verify-nounmatched wire-mock-server))

  (deftest wrapper
    (let [url-1 "/url-1"
          url-2 "/url-2"
          url-3 "/url-3"
          unmatched-url "/unmatched-url"
          param "param"
          value "value"]
      (with-http-mocks wire-mock-server
        [(on-request
           {:method "GET"
            :url    url-1}
           (respond-with
             {:status 200}))
         (on-request
           {:method          "GET"
            :urlPathPattern  url-2
            :queryParameters {:random-param {:equalTo param}}}
           (respond-with
             {:status 200}))
         (on-request
           {:method       "POST"
            :url          url-3
            :bodyPatterns [{:equalToJson {:random value}}]}
           (respond-with
             {:status 201}))]

        @(http-kit/get (str wire-mock-address url-1))

        @(http-kit/get (str wire-mock-address url-2)
           {:query-params {:random-param param}})
        @(http-kit/post (str wire-mock-address url-3)
           {:body (jason/->wire-json {:random value})})

        (testing "server has been called three times"
          (is (true? (verify
                       @wire-mock-server
                       {:method "GET"
                        :url    url-1})))
          (is (true? (verify
                       @wire-mock-server
                       {:method          "GET"
                        :urlPathPattern  url-2
                        :queryParameters {:random-param {:equalTo param}}})))
          (is (true? (verify
                       @wire-mock-server
                       {:method       "POST"
                        :url          url-3
                        :bodyPatterns [{:equalToJson {:random value}}]})))
          (is (true? (verify
                       @wire-mock-server
                       {:method       "POST"
                        :url          unmatched-url} 0)))))))

  (deftest wrapper-scenarios
    (let [url-1 "/url-1"
          url-2 "/url-2"
          param "param"]
      (with-http-mocks wire-mock-server
        [(on-request
           {:method "GET"
            :url    url-1}
           (respond-with
             {:status 200}))
         (on-request
           {:method       "POST"
            :url          url-2
            :bodyPatterns [{:equalToJson {:random param}}]}
           (in-scenario "Scenario 1")
           (in-state "Started")
           (respond-with {:status 503})
           (move-to-state "passing"))

         (on-request
           {:method       "POST"
            :url          url-2
            :bodyPatterns [{:equalToJson {:random param}}]}
           (in-scenario "Scenario 1")
           (in-state "passing")
           (respond-with {:status 201}))]

        @(http-kit/get (str wire-mock-address url-1))

        @(http-kit/post (str wire-mock-address url-2)
           {:body (jason/->wire-json {:random param})})
        @(http-kit/post (str wire-mock-address url-2)
           {:body (jason/->wire-json {:random param})})

        (testing "server has been called three times"
          (is (true? (verify
                       @wire-mock-server
                       {:method "GET"
                        :url    url-1})))
          (is (true? (verify
                       @wire-mock-server
                       {:method          "POST"
                        :urlPathPattern  url-2
                        :bodyPatterns [{:equalToJson {:random param}}]}
                       2))))))))

