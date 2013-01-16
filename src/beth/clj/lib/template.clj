;; # Template Processing
;; Templates are used in the background to help with creating the
;; actual pages. There are two mechanisms to set them up:
;;  - the "include" function will add the stuff of the linked page to
;;    this one.
;;  - the "within" mechanism can be used to decorate a page with other
;;    stuff.
;; Around these two functions there are a lot of helpers for creating
;; stuff in the right pages.

(ns beth.clj.lib.template
  (:require [beth.clj.lib.config    :as config]
            [net.cgrand.enlive-html :as html]))


;; Forward declaration of the actual template processing function.
(declare process-template)


;; ## Helper Functions
;; The following helper functions are required for the acutal black
;; magic done in the following.

(defn load-template
  "Load the template if it exists."
  [filename]
  (let [base-dir (-> (config/lookup :path.snippets)
                     (clojure.java.io/resource))
        file     (clojure.java.io/file base-dir filename)]
    (if (.exists file)
      (html/html-resource file)
      (throw (Exception. (format "Template \"%s\" does not exist!" filename))))))

(defn unwrap-html-body
  "As enlive adds html and body tags to templates (if they don't have
   them) we need to strip them in certain situations."
  [html-nodes]
  (html/at html-nodes
           [:html] html/unwrap
           [:body] html/unwrap))

(defn remove-example-data
  "This is required for snippets to remove example data used to show
   the snippet in action."
  [page-nodes]
  (-> page-nodes
      (html/at [:.example-element]
               nil)
      (html/transform [:.example-data]
                      (html/do-> (html/content nil)
                                 (html/remove-class "example-data")))))


;; ## Handle Includes
;; Includes are used to include a different template into the current
;; one. A good example would be defining header and footer in different
;; and separate files and include them when needed:
;;     <html>
;;       <body>
;;         <_include file="header.html" />
;;         <div id="content">lorem ipsum</div>
;;         <_include file="footer.html" />
;;       </body>
;;     </html>

(defn handle-include
  "Process the actual include node."
  [html-node]
  (let [filename (-> html-node :attrs :file)]
    (-> (load-template filename)
        (process-template)
        (unwrap-html-body))))

(defn handle-includes
  "Collect all includes of the current stage an add them."
  [html-node]
  (html/transform html-node [:_include] handle-include))


;; ## Handle Within
;; Using within one can decorate a template. This is useful to have the
;; application wide frame in a separate file and add subpages that just
;; add the content.
;;     <_within file="application.html" />
;;       <h1 id="someheader">some header</h1>
;;       <div id="content">lorem ipsum</div>
;;     </_within>
;; This would add the content of the two tags given (h1 and div with
;; the respective identifiers) to the respective tags in the
;; application.html file.

(defn- get-node-selector [node]
  (let [id  (-> node :attrs :id)
        tag (:tag node)]
    [(keyword (str (name tag) "#" id))]))

(defn process-within
  "Includes the nodes given in the within tag to the outer template."
  [[node & r]]
  (if (and (nil? r)                  ; there is one node
           (= (:tag node) :_within)) ; with tag _within
    (let [filename (-> node :attrs :file)]
      (loop [outer    (-> (load-template filename)
                          (process-template))
             children (->> node :content (filter map?))]
        (if-let [child (first children)]
          (recur (html/transform outer (get-node-selector child)
                                       (html/substitute child))
                 (rest children))
          outer)))
    (throw (Exception. (str "Erroneous usage of _within tag.")))))

(defn handle-within
  "If the page is wrapped within another page reflect that."
  [html-nodes]
  (let [nodes (html/select html-nodes [:_within])]
    (if (empty? nodes)
      html-nodes ;; aka no _within tag found
      (-> html-nodes
          (unwrap-html-body)
          (process-within)))))


;; ## Template Processing
;; These are the glue methods that will start the magic.

(defn process-template
  "This is mostly useful internally. It is given a html representation
   as returned from the enlive library."
  [html-nodes]
  (-> html-nodes
      (handle-includes)
      (handle-within)))

(defn process-template-file
  "This is the method that you're most probably looking for. Give it the
   path fragment (without the part defined in the template-root config
   variable) contained in the request and get the processed template in
   return."
  [filename]
  (-> (load-template filename)
      (process-template)))

(defn render
  "Will render the page nodes to an actual html string that can be
   served nicely."
  [page-nodes]
  (->> page-nodes
       (html/emit*)
       (apply str)))

