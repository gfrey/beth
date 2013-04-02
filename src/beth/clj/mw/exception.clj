;; # Exception Middleware
;; This middleware tracks down internal server errors. The exception
;; can be used to display some more information in development mode.

(ns beth.clj.mw.exception
  (:require [beth.clj.lib.config    :as config]
            [clojure.tools.logging  :as log]
            [net.cgrand.enlive-html :as html]))


;; ## Helper Methods

(defn log-exception
  "Log an error message using the information from the exception and
   the request."
  [{:keys [request-method uri]} exception]
  (log/error exception
             (format "Exception handling %s request to %s"
                     request-method uri)))

(defn load-stacktrace-snippet []
  (html/snippet
    (format "%s/stacktrace.html" (config/lookup :path.snippets))
    [[:div#content]]
    [exception]
    [:h3#message]    (html/content (.getMessage exception))
    [:ul#strace :li] (html/clone-for
                      [e (.getStackTrace exception)]
                      [:span.source] (html/content
                                      (clojure.string/replace
                                       (.getClassName e)
                                       #"\$" "/"))
                      [:span.file]   (html/content
                                      (.getFileName e))
                      [:span.line]   (html/content
                                      (str (.getLineNumber e))))))


  ;; ## Exception Handler

(defn wrap-exception-handler
  "An exception catching (and printing) middleware that is only loaded
   in development mode."
  [handler server-mode]
  (let [stacktrace (load-stacktrace-snippet)]
    (fn [request]
      (try
        (handler request)
        (catch Exception e
          (log-exception request e)
          {:status    500
           :exception e
           :message   (if (= server-mode :dev)
                        (stacktrace e)
                        "")})))))
