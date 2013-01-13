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
  (:require [aleph.http              :as http]
            [beth.clj.lib.config     :as config]
            [beth.clj.lib.template   :as template]
            [beth.clj.mw.development :as development]
            [beth.clj.mw.error       :as error]
            [beth.clj.mw.exception   :as exception]
            [beth.clj.mw.files       :as files]
            [beth.clj.mw.logging     :as logging]
            [beth.clj.mw.pages       :as pages]))


;; ## Configuration Handling

;; The beth configuration handler.
(def cfg (config/load-config-file "beth.cfg"))

(defn wrap-config-handler
  "A middleware to associate the configuration to the request, so that
   it is available to subsequent handlers."
  [handler]
  (fn [request]
    (-> request
        (assoc :cfg cfg)
        (handler))))


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
      (if (= (get-in response [:headers "Content-Type"]) "text/html")
        (->> (template/render body)
             (assoc response :body))
        response))))


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
  (not-found  {:status 404}))

;; The application routes are loaded from the file given in the
;; configuration.

(defn get-application-routes
  "This is a little dirty hack to make loading of the application
   routes work. This namespace and binding name is read from the
   configuration. To retrieve them the ns-publics (list of public
   bindings of a namespace) is used."
  []
  (let [c     (config/lookup cfg :app.routes)
        [n b] (clojure.string/split c #"/")]
    (require (symbol n))
    (-> (symbol n)
        (ns-publics)
        (get (symbol b)))))

(defn get-routes []
  (routes (get-application-routes)
          system-routes))


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
      (files/wrap-file-handler)
      (development/wrap-development-handler server-mode)
      (exception/wrap-exception-handler)
      (error/wrap-error-handler)
      (logging/wrap-logger)
      (wrap-response-handler)
      (wrap-config-handler)
      (http/wrap-ring-handler)))

