(defproject derby2pg "0.1.0"
  :description "Tool to jumpstart Apache Derby to Postgres data transfer"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [funcool/clojure.jdbc "0.9.0"]
                 [org.apache.derby/derby "10.13.1.1"]
                 ]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.4.0"]]}}
  :main derby2pg.core
  :aot [derby2pg.core]
  :uberjar-name "derby2pg.jar"
)
