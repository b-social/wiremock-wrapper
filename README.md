# b-social/wiremock-wrapper

A Clojure library that wraps the wiremock Java library

## Install

Add the following to your `project.clj` file:

```clj
[b-social/wiremock-wrapper "0.2.3"]
```

## Documentation

* [API Docs](http://b-social.github.io/wiremock-wrapper)

## Usage

```clojure
(let [wire-mock-server (wiremock/new-wire-mock-server)
      test-system (atom (test-system/new-test-system
                          :core-banking-gateway
                          (config/core-banking-gateway-configuration
                            {:core-banking-gateway-base-url
                             ;; A convenient way to define base url for service:
                             ;; e.g. http://localhost:[WIREMOCK_PORT]/core-banking
                             ;; So it's easy to use same WireMock for multiple services with different base url path
                             (service-mock-base-url
                               wire-mock-server
                               :core-banking)})))]

  (use-fixtures :once
                (wiremock/with-wire-mock-server wire-mock-server)
                (test-system/with-system-lifecycle test-system))
  (use-fixtures :each
                (with-empty-database test-system)
                ;; Reset WireMock after each test: mocks, requests journal, etc
                (wiremock/with-empty-wire-mock-server wire-mock-server)
                ;; It will fail a test if any unexpected request was made to the WireMock server
                ;; Could potentially catch additional hard to detect bugs
                (wiremock/with-verify-nounmatched wire-mock-server))

  (deftest test-example
    (let [deposit-id (data/random-uuid)]

      ;; Note: it's not creating a new nesting layer
      ;; All mocks are available on the server until it's cleared by fixture
      (configure-mocks-on
        wire-mock-server
        [
         ;;
         ;; Discovery stub example
         {:request {:urlPath "/core-banking"
                    :method "GET"
                    :headers {"Content-Type" {:equalTo "application/json"}
                              "Accept" {:equalTo "application/hal+json"}}}
          :response {:status 200
                     ;; real body here
                     :body (-> {} (resource->json))}}

         ;; Deposit stub example
         {:request {:url (str "/core-banking/deposits/" deposit-id)
                    :method "PUT"
                    ;; common header are defined as a constant in the wiremock-wrapper library
                    :headers wiremock-wrapper/COMMON-HEADERS
                    :bodyPatterns [{:equalToJson {:amount 1}
                                    :ignoreExtraElements true}]}
          :response {:status 201}}])

      ;; Test body goes here
      ;; And all assertions here

      ;; Good idea to include check for expected N of calls,
      ;; to prevent issue if there were more requests to a same endpoint
      ;; than expected
      (testing "Expected number of request to the WireMock server"
        (is (= 2 (-> (wiremock/get-requests-from @wire-mock-server)
                     (get "requests")
                     (count))))))))
```

## Debugging
In case of stubs not match, WireMock provides useful logs, e.g.:
```
5366377 [qtp1955035115-183] ERROR WireMock -
                                               Request was not matched
                                               =======================

-----------------------------------------------------------------------------------------------------------------------
| Closest stub                                             | Request                                                  |
-----------------------------------------------------------------------------------------------------------------------
                                                           |
GET                                                        | GET
/core-baking                                               | /core-banking                                       <<<<< URL does not match
                                                           |
Content-Type: application/json                             | Content-Type: application/json
Accept: application/hal+json                               | Accept: application/hal+json
                                                           |
                                                           |
-----------------------------------------------------------------------------------------------------------------------

```
So we know in this case that we've made a typo. If this logs are now visible, usually it means that logback wasn't configured properly in test.
Compare it with this one (which should work), usually missing STDOUT appender: https://github.com/b-social/internal-payment-service/blob/master/test/shared/logback-test.xml#L34

## License

Copyright © 2020 Kroo Ltd

Distributed under the terms of the 
[MIT License](http://opensource.org/licenses/MIT).
