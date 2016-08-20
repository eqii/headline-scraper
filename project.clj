(defproject headline-scraper "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.5"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.memoize "0.5.8"]
                 [http-kit "2.2.0"]]
  :main ^:skip-aot headline-scraper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
