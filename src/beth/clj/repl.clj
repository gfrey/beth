;; # Beth REPL
;; This namespace is used only to handle the code reloading from the
;; REPL. As this namespace doesn't define anything it shouldn't
;; require reloading that often. This is essential for the refresh
;; stuff to work properly. See the documentation of the
;; clojure.tools/namespace.repl package.

(ns beth.clj.repl
  (:require [cemerick.piggieback          :as pback]
            [cljs.repl                    :as cljsRepl]
            [cljs.repl.browser            :as cljsBrowser]
            [clojure.repl                 :as cRepl]
            [clojure.tools.namespace.repl :as nRepl]))

;; Don't reload this namespace! This makes sure the refresh function
;; works properly.
(nRepl/disable-reload!)

(defn cljs-repl
  []
  (pback/cljs-repl
   :repl-env (doto (cljsBrowser/repl-env :port 9000)
               cljsRepl/-setup)))

(defn refresh
  "Use this function to refresh the server, for example after changing
   the clojure part of the application. Needs to be given the already
   running server to work properly."
  [server]
  (when server
    (server))
  (let [new-server (nRepl/refresh :after 'beth.clj.server/start-server)]
    (if (fn? new-server)
      new-server
      (cRepl/pst))))

(defn run
  "Run the server and print usage information."
  []
  (println "Starting the server. To refresh enter '(def s (refresh s))'.")
  (refresh nil))

