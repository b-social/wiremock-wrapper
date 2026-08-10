(defproject b-social/wiremock-wrapper "0.2.9-SNAPSHOT"
  :description "A clojure wrapper library around Java wiremock library"
  :url "https://github.com/b-social/wiremock-wrapper"
  :license {:name "The MIT License"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.2"]
                 [http-kit "2.3.0"]
                 [freeport "1.0.0"]
                 [b-social/jason "0.1.5"]
                 [com.github.tomakehurst/wiremock "2.27.2"]
                 [org.clojure/tools.logging "1.2.4"]
                 [medley "1.1.0"]]
  :managed-dependencies [[commons-fileupload/commons-fileupload "1.6.0"]
                         [com.fasterxml.jackson.core/jackson-core "2.18.8"]
                         [net.minidev/json-smart "2.4.9"]
                         [com.fasterxml.jackson.core/jackson-databind "2.18.9"]
                         [org.eclipse.jetty/jetty-webapp "9.4.33.v20201020"]
                         [org.apache.commons/commons-lang3 "3.18.0"]
                         [com.google.guava/guava "20.0.tuxcare"]
                         [org.eclipse.jetty/jetty-servlets "9.4.52"]
                         [org.apache.httpcomponents/httpclient "4.5.13"]
                         [commons-io/commons-io "2.14.0"]
                         [org.eclipse.jetty/jetty-xml "9.4.52.v20230823"]
                         [org.eclipse.jetty/jetty-server "9.4.47"]
                         [org.eclipse.jetty/jetty-http "9.4.47"]]
  :plugins [[lein-eftest "0.5.3"]
            [lein-changelog "0.3.2"]
            [lein-shell "0.5.0"]
            [lein-codox "0.10.7"]]
  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}
  :repl-options {:init-ns wiremock-wrapper}
  :release-tasks
  [["shell" "git" "diff" "--exit-code"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["codox"]
   ["changelog" "release"]
   ["shell" "sed" "-E" "-i" "" "s/\"[0-9]+\\.[0-9]+\\.[0-9]+\"/\"${:version}\"/g" "README.md"]
   ["shell" "git" "add" "."]
   ["vcs" "commit"]
   ["vcs" "tag"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "tag"]
   ["vcs" "push"]])
