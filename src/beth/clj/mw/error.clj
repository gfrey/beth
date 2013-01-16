;; # Error Page Handling

(ns beth.clj.mw.error
  (:require [beth.clj.lib.config    :as config]
            [beth.clj.lib.template  :as template]
            [net.cgrand.enlive-html :as html]))

(def http-status-codes
  {400 "Bad Request"	
   401 "Unauthorized"	
   402 "Payment Required"	
   403 "Forbidden"	
   404 "Not Found"	
   405 "Method Not Allowed"	
   406 "Not Acceptable"	
   407 "Proxy Authentication Required"
   408 "Request Timeout"	
   409 "Conflict"	
   410 "Gone"	
   411 "Length Required"	
   412 "Precondition Failed"	
   413 "Request Entity Too Large"	
   414 "Request-URI Too Long"	
   415 "Unsupported Media Type"	
   416 "Requested Range Not Satisfiable"	
   417 "Expectation Failed"	
   422 "Unprocessable Entity"
   423 "Locked"
   424 "Failed Dependency"
   426 "Upgrade Required"	
   428 "Precondition Required"
   429 "Too Many Requests"
   431 "Request Header Fields Too Large"
   500 "Internal Server Error"
   501 "Not Implemented"
   502 "Bad Gateway"
   503 "Service Unavailable"
   504 "Gateway Timeout"
   505 "HTTP Version Not Supported"
   506 "Variant Also Negotiates (Experimental)"
   507 "Insufficient Storage"
   508 "Loop Detected"
   509 "Unassigned"	
   510 "Not Extended"
   511 "Network Authentication Required"})

(defn insert-reason
  [page status]
  (let [message (get http-status-codes status)
        reason  (format "%s - %s" status message)]
    (-> page
        (html/transform [:h1#error-description]
                        (html/content reason))
        (html/transform [:div#error-message]
                        (html/content "")))))

(defn handle-errors
  [{:keys [status] :as response} cfg]
  (let [snippets-path (config/lookup cfg :path.snippets)
        template      (-> "error/error-page.html"
                          (template/process-template-file cfg)
                          (insert-reason status))]
    (-> response
        (assoc :headers {"Content-Type" "text/html"})
        (assoc :body template))))

(defn wrap-error-handler
  [handler]
  (fn [{:keys [cfg] :as request}]
    (let [{:keys [status] :as response} (handler request)]
      (if (#{404 500} status)
        (handle-errors response cfg)
        response))))