;; # Beth Server
;; This contains beth's core, aka the aleph server listening for requests
;; from the outside world.

(ns beth.clj.server
  (:require [aleph.http            :as http]
            [beth.clj.lib.cljsc    :as cljsc]
            [beth.clj.lib.config   :as config]
            [beth.clj.middleware   :as mw]
            [clojure.tools.logging :as log]))


;; ## Server Handling
;; These function are used to controll the aleph server used by beth.

(defn start-server
  "Start the server. If no mode is set the server will be started in
   production mode. For development mode use :dev."
  ([]
     (start-server :dev))
  ([server-mode]
     (org.apache.log4j.PropertyConfigurator/configure "log4j.properties")
     (log/info (str "Starting the server in " server-mode " mode."))
     (when (= server-mode :prod)
       (log/info "Compiling ClojureScript")
       (cljsc/build mw/cfg :prod))
     (http/start-http-server
      (mw/chain-middleware server-mode)
      {:port 8080 :websocket true})))

(defn stop-server
  "Stop the aleph server by calling the function retured and stored when
   server was started."
  [server]
  (when server
    (log/info "Stopping the server.")
    ((server))))
