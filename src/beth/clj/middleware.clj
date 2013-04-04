;; # Beth Middleware
;; This namespace contains the core middleware handling for beth. This
;; is mostly the glue needed for combining the different middlewares
;; and the distinction between production and development mode.
;;
;; Middleware in this context is a function that will give a maybe
;; modified request to a given handler and returns its response maybe
;; after modifying it.

(ns beth.clj.middleware
  (:require [aleph.http              :as http]
            [beth.clj.lib.config     :as config]
            [beth.clj.lib.template   :as template]
            [beth.clj.mw.development :as development]
            [beth.clj.mw.error       :as error]
            [beth.clj.mw.exception   :as exception]
            [beth.clj.mw.pages       :as pages]
            [clojure.data.json       :as json]
            [clojure.tools.logging   :as log]
            [net.cgrand.moustache    :as mou]
            [ring.middleware.content-type :as ctype]
            [ring.middleware.resource :as resource]
            [ring.middleware.params  :as params]))


;; ## Configuration Handling

(defn wrap-config-handler
  "A middleware to make the configuration available for to subsequent
   handlers. This is done inside config using a dynamic
   binding. Please note that the file is loaded only once during the
   call of the wrapper function. Request handling will use this
   resource for binding."
  [handler]
  (let [cfg (config/load-config-file "beth.cfg")]
    (fn [request]
      (config/with-loaded-config cfg
        (handler request)))))


;; ## Response Handling
;; Responses might need to be cleaned up, for example templates need
;; to be rendered.

(defn wrap-response-handler
  "Make sure that responses containing templates are
   rendered. Template rendering is required so late in the request
   processing chain, as there might be additional steps required to be
   done to the html document, like injection of JavaScript resources."
  [handler]
  (fn [request]
    (let [{:keys [body] :as response} (handler request)]
      (case (get-in response [:headers "Content-Type"])
        "text/html"        (->> (template/render body)
                                (assoc response :body))
        "application/json" (->> (json/write-str body)
                                (assoc response :body))
        response))))


;; ## Logging Middleware

(defn wrap-logger
  "Wrapper that will return a function that will get logging
   information from the request, call the subsequent handlers and
   afterwards print information on the request."
  [handler]
  (fn [request]
    (let [{:keys [uri request-method]}  request
          {:keys [status] :as response} (handler request)
          req-method                    (-> request-method
                                            (name)
                                            (clojure.string/upper-case))
          info-msg                      (str req-method " " uri " " status)]
      (cond
       (= status 404) (log/debug info-msg)
       (= status 500) (log/error info-msg)
       :else          (log/info info-msg))
      response)))


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
(def system-routes
  (mou/app ["snippets" & path]
           (fn [_] (->> path
                       (clojure.string/join "/")
                       (template/get-snippet)
                       (response 200)))))

;; The application routes are loaded from the file given in the
;; configuration.

(defn get-application-routes
  "This is a little dirty hack to make loading of the application
   routes work. This namespace and binding name is read from the
   configuration. To retrieve them the ns-publics (list of public
   bindings of a namespace) is used."
  []
  (let [c     (config/lookup :app.routes)
        [n b] (clojure.string/split c #"/")]
    (require (symbol n))
    (-> (symbol n)
        (ns-publics)
        (get (symbol b)))))

(defn get-routes []
  (let [app-routes (get-application-routes)]
    (mou/app ["app" &] app-routes
             [&]       system-routes)))


;; ## Middleware Chaining
;; The different middlewares are forming something similar to an
;; onion, where each layer somehow modifies request and/or response
;; prior to handing the respective entity to the respective layer.

(defn chain-middleware
  "Function that will combine all required middlewares giving respect
   to the server mode given (either :prod or :dev)."
  [server-mode]
  (-> (get-routes)
      (pages/wrap-page-handler server-mode)
      (params/wrap-params)
      (resource/wrap-resource "files")
      (development/wrap-development-handler server-mode)
      (exception/wrap-exception-handler server-mode)
      (error/wrap-error-handler)
      (wrap-logger)
      (ctype/wrap-content-type)
      (wrap-response-handler)
      (wrap-config-handler)
      (http/wrap-ring-handler)))
