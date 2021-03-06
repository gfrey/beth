;; # Page Handling Middleware
;; Pages are templates that contain actual content serverd to the
;; public.

(ns beth.clj.mw.pages
  (:require [beth.clj.lib.config    :as config]
            [beth.clj.lib.template  :as template]
            [net.cgrand.enlive-html :as html]))


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
  [{:keys [uri]}]
  (let [root (-> (config/lookup :path.pages)
                 (clojure.java.io/resource))
        path (subs uri 1)
        file (get-page root path)]
    (when (.isFile file)
      file)))

(defn get-pagename
  "Returns the name of the given page, i.e. the filename without path
   and suffix."
  [page]
  (-> page
      (.getName)
      (clojure.string/replace-first #".html$" "")))

(defn create-response
  "Creates a dummy response with the body set to the given file and status
   200."
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})


;; ## Fragment Injection
;; The pages created from templates need to be spiced up with
;; JavaScript and CSS. This is done in the following function taking
;; values from the configuration and putting them in the right
;; locations in the pages.

(def style-node
  (html/html-snippet "<link type=\"text/css\" rel=\"stylesheet\">"))

(def script-node
  (html/html-snippet "<script type='text/javascript'></script>"))

(defn inject-css
  [page css]
  (->> css
       (map #(html/transform style-node [:link] (html/set-attr :href %)))
       (apply html/append)
       (html/transform page [:head])))

(defn inject-js
  [page js]
  (->> js
       (map #(html/transform script-node [:script] (html/set-attr :src %)))
       (apply html/append)
       (html/transform page [:head])))

(defn inject-script
  [page script]
  (->> script
       (map #(html/transform script-node [:script] (html/content %)))
       (apply html/append)
       (html/transform page [:body])))

(defn find-injectables
  "Find the injection configuration for the given mode and page. If no
   configuration for the page exists the default will be used."
  [pagename mode]
  (if-let [key (config/has-key? :injector mode pagename)]
    (config/lookup key)
    (config/lookup :injector mode)))

(defn fragment-injection
  "The dispatcher on the different things to inject."
  [page pagename mode]
  (let [{:keys [css js script]} (find-injectables pagename mode)]
    (-> page
        (inject-css css)
        (inject-js js)
        (inject-script script))))


;; ## Page Handler
;; This will check whether the request matches a page and if so load
;; that page and create a proper response.

(defn wrap-page-handler
  "The page handling middleware."
  [handler server-mode]
  (fn [request]
    (if-let [page (is-page? request)]
      (-> page
          (template/process-template-file)
          (fragment-injection (get-pagename page) server-mode)
          (create-response))
      (handler request))))
