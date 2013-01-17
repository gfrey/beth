(ns beth.clj.lib.template-test
  (:use [beth.clj.lib.template]
        [clojure.test]
        [midje.sweet])
  (:require [beth.clj.lib.config    :as config]
            [net.cgrand.enlive-html :as html]))

;; Some background for setting the configuration as required.
(background
 (around :facts
         (config/with-loaded-config
           {:path.snippets "test/template"}
           ?form)))

(deftest load-template-test
  (facts
   (-> (load-template "test.html")
       (html/select [:html :body :div#content]))
   => [{:tag     :div
        :attrs   {:id "content"}
        :content ["foobar"]}]

   (load-template "missing.html")
   => (throws Exception "Template \"missing.html\" does not exist!")))

(deftest remove-example-data-test
  (facts
   ;; Verify validity of the example data file.
   (-> (load-template "example-data.html")
       (html/select [:html :body :div#content :ul :li]))
   => [{:tag     :li
        :attrs   {:class "example-data"}
        :content ["foobar"]}]

   ;; Same stuff, but now remove example data.
   (-> (load-template "example-data.html")
       (remove-example-data)
       (html/select [:html :body :div#content :ul :li]))
   => [{:tag     :li
        :attrs   {}
        :content []}]))

(deftest handle-includes-test
  (facts
   (-> (load-template "include.html")
       (handle-includes)
       (html/select [:div#wrapper :> :div]))
   => [{:tag :div :attrs {:id "header"}  :content ["header"]}
       {:tag :div :attrs {:id "content"} :content ["foobar"]}
       {:tag :div :attrs {:id "footer"}  :content ["footer"]}]

   (-> (html/html-snippet "<_include file=\"missing.html\" />")
       (handle-includes))
   => (throws Exception "Template \"missing.html\" does not exist!")))

(def erroneous-within-statements
  {:not-first   (str "<p>something</p>"
                     "<_within file=\"test.html\" />")
   :not-one     (str "<_within file=\"foo.html\" />"
                     "<_within file=\"bar.html\" />")})

(deftest handle-within-test
  (facts
   (-> (load-template "within.html")
       (handle-within)
       (html/select [:html :body :div#content]))
   => [{:tag :div, :attrs {:id "content"}, :content ["killroy"]}]

   (-> (html/html-snippet (:not-first erroneous-within-statements))
       (handle-within))
   => (throws Exception "Erroneous usage of _within tag.")

   (-> (html/html-snippet (:not-one erroneous-within-statements))
       (handle-within))
   => (throws Exception "Erroneous usage of _within tag.")))

(deftest handle-mixed-templates-test
  (facts "Template without \"within\" or \"include\" tag."
         (-> (load-template "test.html")
             (process-template)
             (html/select [:body :> :div]))
         => [{:tag :div :attrs {:id "header"}  :content ["header"]}
             {:tag :div :attrs {:id "content"} :content ["foobar"]}
             {:tag :div :attrs {:id "footer"}  :content ["footer"]}])

  (facts "Templates with \"within\" and \"include\" tags intermixed."
         (-> (load-template "mixed_within.html")
             (process-template)
             (html/select [:body :> :div]))
         => [{:tag :div :attrs {:id "header"}  :content ["header"]}
             {:tag :div :attrs {:id "content"} :content [{:tag   :p
                                                          :attrs nil
                                                          :content ["killroy"]}
                                                         "\n"]}
             {:tag :div :attrs {:id "footer"}  :content ["footer"]}]

         (-> (load-template "mixed_include.html")
             (process-template)
             (html/select [:body :> :div#wrapper :> :div]))
         => [{:tag :div :attrs {:id "header"}  :content ["header"]}
             {:tag :div :attrs {:id "content"} :content ["killroy"]}
             {:tag :div :attrs {:id "footer"}  :content ["footer"]}]))
