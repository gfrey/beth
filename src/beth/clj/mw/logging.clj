;; # Logging Middleware
;; This middleware will log all incoming requests and the according
;; response status code. Depending on the status code the log level
;; will be set to either info, debug or error.

(ns beth.clj.mw.logging
  (:require [clojure.tools.logging :as log]))


;; ## Helper Methods

(defn request-method->string
  "Return the request method (given as lowercase keyword) as uppercase
  string."
  [request-method]
  (-> request-method
      (name)
      (clojure.string/upper-case)))


;; ## Logging Middleware

(defn wrap-logger
  "Wrapper that will return a function that will get logging
   information from the request, call the subsequent handlers and
   afterwards print information on the request."
  [handler]
  (fn [request]
    (let [{:keys [uri request-method]}  request
          {:keys [status] :as response} (handler request)
          req-method                    (request-method->string request-method)
          info-msg                      (str req-method " " uri " " status)]
      (cond
       (= status 404) (log/debug info-msg)
       (= status 500) (log/error info-msg)
       :else          (log/info info-msg))
      response)))
