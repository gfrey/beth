;; # File Handling Middleware
;; This middleware is responsible for exporting all the static content
;; located on the page.
(ns beth.clj.mw.files
  (:require [beth.clj.lib.config :as config]))


;; ## Mime Type Handling

;; The following map defines a set of mime types that are considered
;; known and are added automatically to files.
(def mime-types
  {"aac"   "audio/aac"
   "atom"  "application/atom+xml"
   "avi"   "video/x-msvideo"
   "bmp"   "image/bmp"
   "css"   "text/css"
   "flv"   "video/x-flv"
   "flac"  "audio/flac"
   "gif"   "image/gif"
   "htm"   "text/html"
   "html"  "text/html"
   "ico"   "image/x-icon"
   "jpe"   "image/jpeg"
   "jpeg"  "image/jpeg"
   "jpg"   "image/jpeg"
   "js"    "text/javascript"
   "json"  "application/json"
   "mov"   "video/quicktime"
   "m4v"   "video/mp4"
   "mp3"   "audio/mpeg"
   "mp4"   "video/mp4"
   "mpe"   "video/mpeg"
   "mpeg"  "video/mpeg"
   "mpg"   "video/mpeg"
   "oga"   "audio/ogg"
   "ogg"   "audio/ogg"
   "ogv"   "video/ogg"
   "pdf"   "application/pdf"
   "png"   "image/png"
   "qt"    "video/quicktime"
   "rss"   "application/rss+xml"
   "rtf"   "application/rtf"
   "sgm"   "text/sgml"
   "sgml"  "text/sgml"
   "svg"   "image/svg+xml"
   "swf"   "application/x-shockwave-flash"
   "tif"   "image/tiff"
   "tiff"  "image/tiff"
   "txt"   "text/plain"
   "xml"   "text/xml"
   "zip"   "application/zip"})

(defn handle-mime-type
  "This function will determine the according mime type for the file
   encapsulated in the response."
  [response]
  (let [file-ext (->> (:body response)
                      (.getName)
                      (re-matches #".*\.(\w+)$")
                      (second))]
    (if-let [mtype (get mime-types file-ext)]
      (assoc-in response [:headers "Content-Type"] mtype)
      response)))


;; ## Helper Methods
;; The following methods are required for the file serving middleware.

(defn is-file?
  "Creates a function that returns a java.io.File object if the given path
   is within the directory returned by the src-dir funtion."
  [{:keys [uri]}]
  (let [root (-> (config/lookup :path.files)
                 (clojure.java.io/resource))
        path (subs uri 1)
        file (clojure.java.io/file root path)]
    (when (.isFile file)
      file)))

(defn create-response
  "Creates a dummy response with the body set to the given file and status
   200."
  [file]
  (handle-mime-type {:status 200 :headers {} :body file}))


;; ## Static File Serving

;; The following middleware will server static files from the /public
;; folder.
(defn wrap-file-handler
  "The actual middleware."
  [handler]
  (fn [request]
    (if-let [file (is-file? request)]
      (create-response file)
      (handler request))))
