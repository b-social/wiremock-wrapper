(ns wiremock-wrapper.httpkit-fake-test
  (:require [clojure.test :refer [deftest testing is]]
            [wiremock-wrapper.httpkit-fake :as httpkit]
            [wiremock-wrapper :as wiremock])
  (:import [clojure.lang ExceptionInfo]))

(deftest converts-stub-method
  (testing ":get"
    (is (=
          (httpkit/->wiremock-stub {} [{:method :get} "response-object"])
          {:request  {:method "GET"}
           :response "response-object"})))

  (testing ":post"
    (is (=
          (httpkit/->wiremock-stub {} [{:method :post} {}])
          {:request  {:method "POST"}
           :response {}}))))

(deftest removes-wiremock-base-url
  (let [wiremock-server (wiremock/new-wire-mock-server)
        base-url (wiremock/base-url wiremock-server)
        path "/path-to-resource"
        absolute-url (str base-url path)]

    (testing "has URL with base removed"
      (is (=
            (httpkit/->wiremock-stub wiremock-server
              [{:url absolute-url} "response-object"])
            {:request  {:urlPath path}
             :response "response-object"})))

    (testing "doesnt change URL if no base-url present"
      (is (=
            (httpkit/->wiremock-stub wiremock-server
              [{:url path} "response-object"])
            {:request  {:urlPath path}
             :response "response-object"})))))

(deftest matches-headers-individually
  (testing "adds equalTo for each field"
    (is (=
          (httpkit/->wiremock-stub {}
            [{:headers {:something      "some-value"
                        :something-else "other-value"}}
             "response-object"])
          {:request
           {:headers {:something      {:equalTo "some-value"}
                      :something-else {:equalTo "other-value"}}}
           :response "response-object"}))))

(deftest matches-body-fields-individually
  (testing "matches single field in body"
    (is (=
          (httpkit/->wiremock-stub {}
            [{:body {:something "some-value"}}
             "response-object"])
          {:request
           {:bodyPatterns [{:matchesJsonPath {:expression "?.something"
                                              :equalTo    "some-value"}}]}
           :response "response-object"})))

  (testing "matches multiple field in body"
    (is (=
          (httpkit/->wiremock-stub {}
            [{:body {:something      "some-value"
                     :something-else "other-value"}}
             "response-object"])
          {:request
           {:bodyPatterns [{:matchesJsonPath {:expression "?.something"
                                              :equalTo    "some-value"}}
                           {:matchesJsonPath {:expression "?.something-else"
                                              :equalTo    "other-value"}}]}
           :response "response-object"})))

  (testing "matches nested fields in body"
    (is (=
          (httpkit/->wiremock-stub {}
            [{:body {:some           {:thing {:nested {:deep "some-value"}}}
                     :something-else "other-value"}}
             "response-object"])
          {:request
           {:bodyPatterns [{:matchesJsonPath {:expression "?.some.thing.nested.deep"
                                              :equalTo    "some-value"}}
                           {:matchesJsonPath {:expression "?.something-else"
                                              :equalTo    "other-value"}}]}
           :response "response-object"}))))


(deftest matches-basic-auth-in-headers
  (testing "basic auth"
    (is (=
          (httpkit/->wiremock-stub {}
            [{:basic-auth ["some-user" "some-secret-password"]}
             "response-object"])
          {:request
           {:basicAuth {:username "some-user"
                        :password "some-secret-password"}}
           :response "response-object"}))))

(deftest matches-oauth-in-headers
  (testing "oauth token"
    (is (=
          (httpkit/->wiremock-stub {}
            [{:oauth-token "some-secret-auth-token"}
             "response-object"])
          {:request
           {:headers {:Authorization {:equalTo "Bearer some-secret-auth-token"}}}
           :response "response-object"})))


  (testing "oauth-token-with-other-headers"
    (is (=
          (httpkit/->wiremock-stub {}
            [{:headers     {:something      "some-value"
                            :something-else "other-value"}
              :oauth-token "some-secret-auth-token"}
             "response-object"])
          {:request
           {:headers {:something      {:equalTo "some-value"}
                      :something-else {:equalTo "other-value"}
                      :Authorization  {:equalTo "Bearer some-secret-auth-token"}}}
           :response "response-object"}))))

(deftest converts-many-stubs-at-once
  (let [wiremock-server (wiremock/new-wire-mock-server)
        base-url (wiremock/base-url wiremock-server)
        path "/path-to-resource"
        absolute-url (str base-url path)]

    (testing "converts all stubs"
      (is (=
            (httpkit/->wiremock-stubs wiremock-server
              [{:method :get} "response-object"]
              [{:url absolute-url} "response-object"]
              [{:headers {:something      "some-value"}} "response-object"])

            [{:request  {:method "GET"}
              :response "response-object"}
             {:request  {:url path}
              :response "response-object"}

             {:request
              {:headers {:something      {:equalTo "some-value"}}}
              :response "response-object"}])))))

(deftest cannot-convert-function-stubs
  (testing "converts all stubs"
    (is (thrown-with-msg? ExceptionInfo
          #"Unable to transform function matcher"

          (httpkit/->wiremock-stub {}
            [(fn [_] true) "response-object"])))))

(deftest converts-many-stubs-at-once
  (let [wiremock-server (wiremock/new-wire-mock-server)
        base-url (wiremock/base-url wiremock-server)
        path "/path-to-resource"
        absolute-url (str base-url path)]

    (testing "converts all stubs"
      (is (=
            (httpkit/->wiremock-stubs wiremock-server
              [{:method :get} "response-object"]
              [{:url absolute-url} "response-object"]
              [{:headers {:something      "some-value"}} "response-object"])

            [{:request  {:method "GET"}
              :response "response-object"}
             {:request  {:urlPath path}
              :response "response-object"}

             {:request
              {:headers {:something      {:equalTo "some-value"}}}
              :response "response-object"}])))))

