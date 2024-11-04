(ns wiremock-wrapper.core-test
  (:require [clojure.test :refer [use-fixtures
                                  deftest
                                  is
                                  testing]]
            [org.httpkit.client :as http-kit]
            [wiremock-wrapper :as wiremock-wrapper]
            [jason.convenience :as jason]))

(let [wire-mock-server-atom (wiremock-wrapper/new-wire-mock-server)
      wire-mock-address (wiremock-wrapper/base-url wire-mock-server-atom)]
  (use-fixtures :once
    (wiremock-wrapper/with-wire-mock-server wire-mock-server-atom))
  (use-fixtures :each
    (wiremock-wrapper/with-empty-wire-mock-server wire-mock-server-atom)
    (wiremock-wrapper/with-verify-nounmatched wire-mock-server-atom))

  (deftest wrapper
    (let [url-1 "/url-1"
          url-2 "/url-2"
          url-3 "/url-3"
          unmatched-url "/unmatched-url"
          param "param"
          value "value"]
      (let [stubs
            (wiremock-wrapper/configure-mocks-on
              wire-mock-server-atom
              [{:request {:method "GET"
                          :url url-1}
                :response {:status 200}}
               {:request {:method "GET"
                          :urlPathPattern url-2
                          :queryParameters {:random-param {:equalTo param}}}
                :response {:status 200}}
               {:request {:method "POST"
                          :url url-3
                          :bodyPatterns [{:equalToJson {:random value}}]}
                :response {:status 201}}])]
        (is (= 3 (count stubs)))
        (is (= #{"id" "request" "response" "uuid"}
               (set (keys (first stubs))))))

      @(http-kit/get (str wire-mock-address url-1))

      @(http-kit/get (str wire-mock-address url-2)
                     {:query-params {:random-param param}})

      @(http-kit/post (str wire-mock-address url-3)
                      {:body (jason/->wire-json {:random value})})

      (testing "server has been called three times"
        (is (true? (wiremock-wrapper/verify
                     @wire-mock-server-atom
                     {:method "GET"
                      :url    url-1})))
        (is (true? (wiremock-wrapper/verify
                     @wire-mock-server-atom
                     {:method          "GET"
                      :urlPathPattern  url-2
                      :queryParameters {:random-param {:equalTo param}}})))
        (is (true? (wiremock-wrapper/verify
                     @wire-mock-server-atom
                     {:method       "POST"
                      :url          url-3
                      :bodyPatterns [{:equalToJson {:random value}}]})))
        (is (true? (wiremock-wrapper/verify
                     @wire-mock-server-atom
                     {:method       "POST"
                      :url          unmatched-url} 0))))))

  (deftest wrapper-scenarios
    (let [url-1 "/url-1"
          url-2 "/url-2"
          param "param"]
      (wiremock-wrapper/with-http-mocks wire-mock-server-atom
        [(wiremock-wrapper/on-request
           {:method "GET"
            :url    url-1}
           (wiremock-wrapper/respond-with
             {:status 200}))
         (wiremock-wrapper/on-request
           {:method       "POST"
            :url          url-2
            :bodyPatterns [{:equalToJson {:random param}}]}
           (wiremock-wrapper/in-scenario "Scenario 1")
           (wiremock-wrapper/in-state "Started")
           (wiremock-wrapper/respond-with {:status 503})
           (wiremock-wrapper/move-to-state "passing"))

         (wiremock-wrapper/on-request
           {:method       "POST"
            :url          url-2
            :bodyPatterns [{:equalToJson {:random param}}]}
           (wiremock-wrapper/in-scenario "Scenario 1")
           (wiremock-wrapper/in-state "passing")
           (wiremock-wrapper/respond-with {:status 201}))]

        @(http-kit/get (str wire-mock-address url-1))

        @(http-kit/post (str wire-mock-address url-2)
           {:body (jason/->wire-json {:random param})})
        @(http-kit/post (str wire-mock-address url-2)
           {:body (jason/->wire-json {:random param})})

        (testing "server has been called three times"
          (is (true? (wiremock-wrapper/verify
                       @wire-mock-server-atom
                       {:method "GET"
                        :url    url-1})))
          (is (true? (wiremock-wrapper/verify
                       @wire-mock-server-atom
                       {:method          "POST"
                        :urlPathPattern  url-2
                        :bodyPatterns [{:equalToJson {:random param}}]}
                       2))))))))

