;; # Page Handling Middleware
;; Pages are templates that contain actual content serverd to the
;; public.

(ns beth.clj.mw.pages
  (:require [beth.clj.lib.config   :as config]
            [beth.clj.lib.template :as template]))


;; ## Helper Methods
;; The following methods are required for the file serving middleware.

(defn get-page
  "For an directory <root>/<path> return an file object pointing to it if
   it exists. Otherwise return an file object pointing to the location at
   <root>/<path>/index.html."
  [root path]
  (let [file (clojure.java.io/file root path)]
    (if-not (.isFile file)
      (clojure.java.io/file file "index.html")
      file)))

(defn is-page?
  "Validates whether the given request asks for a page that is stored
   in pages directory specified in the configuration."
  [{:keys [uri cfg]}]
  (let [root (-> (config/lookup cfg :path.pages)
                 (clojure.java.io/resource))
        path (subs uri 1)
        file (get-page root path)]
    (when (.isFile file)
      file)))

(defn create-response
  "Creates a dummy response with the body set to the given file and status
   200."
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})


;; ## Page Handler
;; This will check whether the request matches a page and if so load
;; that page and create a proper response.

(defn wrap-page-handler
  "The page handling middleware."
  [handler]
  (fn [{:keys [cfg] :as request}]
    (if-let [page (is-page? request)]
      (-> page
          (template/process-template-file cfg)
          (create-response))
      (handler request))))
