;; # Beth Middleware
;; This namespace contains the core middleware handling for beth. This
;; is mostly the glue needed for combining the different middlewares
;; and the distinction between production and development mode.
;;
;; Middleware in this context is a function that will give a maybe
;; modified request to a given handler and returns its response maybe
;; after modifying it.

(ns beth.clj.middleware
  (:use [compojure.core  :only [GET defroutes routes]]
        [compojure.route :only [not-found]])
  (:require [aleph.http          :as http]
            [beth.clj.mw.logging :as logging]))


;; ## Helper Functions

(defn response
  "Simple response creation. For a given status, content-type and
   content return the according response. "
  ([status content]
     (response status :body content))
  ([status content-key content]
     {:status status
      :headers {"Content-Type" "text/html"}
      content-key content}))


;; ## Route Dispatching

;; The routing for requests to beth.
(defroutes system-routes
  (GET "/" [] (response 200 "<h1>Welcome To Beth</h1>"))
  (not-found (response 404 :body "Not found!")))


;; ## Middleware Chaining
;; The different middlewares are forming something similar to an
;; onion, where each layer somehow modifies request and/or response
;; prior to handing the respective entity to the respective layer.

(defn chain-middleware
  "Function that will combine all required middlewares giving respect
   to the server mode given (either :production or :development)."
  [server-mode]
  (-> system-routes
      (logging/wrap-logger)
      (http/wrap-ring-handler)))

