;; # Exception Middleware
;; This middleware tracks down internal server errors. The exception
;; can be used to display some more information in development mode.

(ns beth.clj.mw.exception
  (:require [clojure.tools.logging  :as log]))


;; ## Helper Methods

(defn log-exception
  "Log an error message using the information from the exception and
   the request."
  [{:keys [request-method uri]} exception]
  (log/error exception
             (format "Exception handling %s request to %s"
                     request-method uri)))


;; ## Exception Handler

(defn wrap-exception-handler
  "An exception catching (and printing) middleware that is only loaded
   in development mode."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log-exception request e)
        {:status 500
         :exception e
         :message "Internal server error!"}))))
