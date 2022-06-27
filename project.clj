(defproject b-social/wiremock-wrapper "0.2.0"
  :description "A clojure wrapper library around Java wiremock library"
  :url "https://github.com/b-social/wiremock-wrapper"
  :license {:name "The MIT License"
            :url  "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [http-kit "2.3.0"]
                 [freeport "1.0.0"]
                 [b-social/jason "0.1.5"]
                 [com.github.tomakehurst/wiremock "2.27.2"]]
  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}
  :repl-options {:init-ns wiremock-wrapper})