;; # Example Application Routing
;; This namespace contains all the routing for the example
;; application.
(ns example.clj.routing
  (:use [compojure.core  :only [GET defroutes]]
        [compojure.route :only [not-found]]))


;; The actual compojure routing table for the application. Please note
;; that you should not overwrite the Beth specific routes by adding a
;; generic route like "not-found" that will catch away all requests
;; from the system routes.
(defroutes routes
  (GET "/example" [] {:status 200
                      :headers {"Content-Type" "text/html"}
                      :body "<h1>Welcome To Example App</h1>"})
  (GET "/exception" [] (throw (Exception. "An example exception."))))