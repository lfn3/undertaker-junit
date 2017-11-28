(defproject net.lfn3/undertaker-junit "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[net.lfn3/undertaker "0.1.0-SNAPSHOT"]
                 [junit "4.12"]]

  :plugins [[lein-junit "1.1.8"]]

  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/java"]
  :junit ["src/test/java"]
  :java-source-paths ["src/main/java"]

  :target-path "target/"

  :aot [net.lfn3.undertaker.junit.source-rule]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.9.0-RC2"]]}
             :test {:java-source-paths ^:replace ["src/test/java"]}}
  :aliases {"junit" ["do" ["clean"] ["compile"] ["with-profile" "test,provided" "junit"]]})
