(defproject beth "0.1.0-SNAPSHOT"
  :description "Clojure[Script] WebApplication Development Framework"
  :url "https://github.com/gfrey/beth"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[aleph                       "0.3.0-beta8"]
                 [compojure                   "1.1.3"]
                 [log4j/log4j                 "1.2.16"
                                  :exclusions [javax.mail/mail
                                               javax.jms/jms
                                               com.sun.jdmk/jmxtools
                                               com.sun.jmx/jmxri]]
                 [midje                       "1.4.0"]
                 [org.clojure/clojure         "1.4.0"]
                 [org.clojure/tools.logging   "0.2.4"]
                 [org.clojure/tools.namespace "0.2.2"]]
  :main beth.clj.core
  :repl-options {:init-ns beth.clj.repl
                 :init (def s (run))}
  :resource-paths ["resources"])
