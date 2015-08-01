(defproject beth "0.1.0-SNAPSHOT"
  :description "Clojure[Script] WebApplication Development Framework"
  :url "https://github.com/gfrey/beth"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[aleph "0.4.0"]

                 [enlive "1.1.6"]
                 [log4j/log4j "1.2.16" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [midje "1.7.0"]
                 [net.cgrand/moustache "1.2.0-alpha2"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.28"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [ring/ring-core "1.4.0"]]
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]]}}
  :main beth.clj.core
  :resource-paths ["resources"])
