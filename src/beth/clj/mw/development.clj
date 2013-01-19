;; # Development Middlewares
;; As this framework should help the different parties in creating web
;; applications this middleware will add helpers in development mode.

(ns beth.clj.mw.development
  (:require [beth.clj.lib.cljsc     :as cljsc]
            [beth.clj.lib.config    :as config]
            [beth.clj.lib.template  :as template]
            [net.cgrand.enlive-html :as html]))


;; ## ClojureScript Compilation
;; The following methods are used to determine whether any of the
;; ClojureScript file has changed and a recompile is required though.

(defn get-latest-change
  "From all the ClojureScript sources determine the one that changed
   last. If that is newer than the main build artefact (the dependency
   file) than a rebuild is required."
  []
  (->> [(config/lookup :cljs-src)]
       (map clojure.java.io/file)
       (mapcat file-seq)
       (filter #(.isFile %))
       (map #(.lastModified %))
       (apply max)))

(defn wrap-cljs-compiler
  "Middleware that determines whether the ClojureScript code changed
   and a recompile is necessary."
  [handler]
  (fn [request]
    (let [odir  (config/lookup :cljs-dir :dev)
          ofile (config/lookup :cljs-file :dev)]
      (when (< (-> (str odir "/" ofile)
                   (clojure.java.io/file)
                   (.lastModified))
               (get-latest-change))
        (cljsc/build :dev))
      (handler request))))


;; ## Snippet Handling
;; Snippets are fragments of templates that must be served in
;; development mode so that the designer has a possibility to review
;; them.

;; TODO This should be generalized!
(defn- create-response
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

;; TODO Maybe it'd be better to generate some default context, instead
;;      using a page.
(defn get-snippet
  "Load the snippet for the given path, put it into the correct
   environment, and return an proper response."
  [path]
  (let [root (-> (config/lookup :path.snippets)
                 (clojure.java.io/resource))
        file (clojure.java.io/file root path)]
    (if (.isFile file)
      (-> (html/html-resource file)
          (html/at [:html :body] (html/wrap :_within {:file "application.html"})
                   [:html :body] (html/wrap :div {:id "content"}))
          (template/process-template)
          (create-response))
      (throw (Exception. (format "Snippet %s does not exist!" path))))))

(defn wrap-preview-handler
  "The middleware that will fetch requests for snippets and return
   them accordingly."
  [handler]
  (fn [request]
    (let [uri  (:uri request)
          path (->> (clojure.string/split uri #"/")
                    (remove empty?))]
      (if (= (first path) "preview")
        (get-snippet (clojure.string/join "/" (rest path)))
        (handler request)))))


;; ## Development Middlewares
;; This handler will add stuff to the middleware chain that is
;; required during development. That is the reason why the server-mode
;; is relevant to this function. Keep in mind that this function is
;; called only at server startup and therefore imposes no loss in
;; performance on the actual application in production mode.

(defn wrap-development-handler
  "The development mode middlewares"
  [handler server-mode]
  (if (#{:dev} server-mode)
    (-> handler
        (wrap-preview-handler)
        (wrap-cljs-compiler))
    handler))
