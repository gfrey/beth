;; # Beth ClojureScript Browser REPL
;; This namespace provides the tools required for the browser REPL.

(ns beth.cljs.core.repl
  (:require [clojure.browser.repl :as repl]))


(defn ^:export connect
  "Connect to the server started on port 9000."
  []
  (repl/connect "http://localhost:9000/repl"))
