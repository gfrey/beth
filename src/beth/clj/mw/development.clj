;; # Development Middlewares
;; As this framework should help the different parties in creating web
;; applications this middleware will add helpers in development mode.

(ns beth.clj.mw.development
  (:require [beth.clj.lib.cljsc  :as cljsc]
            [beth.clj.lib.config :as config]))


;; ## ClojureScript Compilation
;; The following methods are used to determine whether any of the
;; ClojureScript file has changed and a recompile is required though.

(defn get-latest-change
  "From all the ClojureScript sources determine the one that changed
   last. If that is newer than the main build artefact (the dependency
   file) than a rebuild is required."
  [cfg]
  (->> [(config/lookup cfg :cljs-src)]
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
    (let [cfg   (:cfg request)
          odir  (config/lookup cfg :cljs-dir :dev)
          ofile (config/lookup cfg :cljs-file :dev)]
      (when (< (-> (str odir "/" ofile)
                   (clojure.java.io/file)
                   (.lastModified))
               (get-latest-change cfg))
        (cljsc/build cfg :dev))
      (handler request))))


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
        (wrap-cljs-compiler))
    handler))
