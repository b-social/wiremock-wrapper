
(ns wiremock-wrapper.unit-test
  (:require [clojure.test :refer :all]
            [freeport.core :refer [get-free-port!]]
            [org.httpkit.client :as http-kit]
            [wiremock-wrapper :refer :all])
  (:import [clojure.lang ExceptionInfo]))

(let [wire-mock-server-port (get-free-port!)
      wire-mock-config (wire-mock-config wire-mock-server-port)
      wire-mock-server (new-wire-mock-server wire-mock-config)
      wire-mock-address (base-url wire-mock-server-port)]
  (use-fixtures :once
    (with-wire-mock-server wire-mock-server))
  (use-fixtures :each
    (with-empty-wire-mock-server wire-mock-server))
  (deftest unmatched-url-throws-exception

    (is (thrown? ExceptionInfo
          ((with-verify-nounmatched wire-mock-server)
            (fn []  @(http-kit/get (str wire-mock-address "/url"))))))))

