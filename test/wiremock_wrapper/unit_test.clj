(ns wiremock-wrapper.unit-test
  (:require [clojure.test :refer [use-fixtures
                                  deftest is]]
            [org.httpkit.client :as http-kit]
            [wiremock-wrapper :as wiremock-wrapper])
  (:import [clojure.lang ExceptionInfo]))

(let [wire-mock-server-atom (wiremock-wrapper/new-wire-mock-server)
      wire-mock-address (wiremock-wrapper/base-url wire-mock-server-atom)]
  (use-fixtures :once
                (wiremock-wrapper/with-wire-mock-server wire-mock-server-atom))
  (use-fixtures :each
                (wiremock-wrapper/with-empty-wire-mock-server wire-mock-server-atom))
  (deftest unmatched-url-throws-exception
    (is (thrown? ExceptionInfo
                 ((wiremock-wrapper/with-verify-nounmatched wire-mock-server-atom)
                  (fn []  @(http-kit/get (str wire-mock-address "/url"))))))))

