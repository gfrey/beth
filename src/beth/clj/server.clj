;; # Beth Server
;; This contains beth's core, aka the aleph server listening for requests
;; from the outside world.

(ns beth.clj.server
  (:require [aleph.http          :as http]
            [beth.clj.middleware :as mw]))


;; ## Server Handling
;; These function are used to controll the aleph server used by beth.

(defn start-server
  "Start the server. If no mode is set the server will be started in
   production mode. For development mode use :development."
  ([]
     (start-server :development))
  ([server-mode]
     (http/start-http-server
      (mw/chain-middleware server-mode)
      {:port 8080 :websocket true})))

(defn stop-server
  "Stop the aleph server by calling the function retured and stored when
   server was started."
  [server]
  (when server
    ((server))))
