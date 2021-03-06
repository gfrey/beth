;; # Example Application Routing
;; This namespace contains all the routing for the example
;; application.
(ns example.clj.routing
  (:require [example.clj.model :as model]
            [compojure.core :refer :all]))


;; The actual compojure routing table for the application. Please note
;; that you should not overwrite the Beth specific routes by adding a
;; generic route like "not-found" that will catch away all requests
;; from the system routes.
(def app-routes
  (routes
    (GET "/exception" [] (throw (Exception. "An example exception.")))))