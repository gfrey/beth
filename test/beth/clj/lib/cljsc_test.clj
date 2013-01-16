(ns beth.clj.lib.cljsc-test
  (:require [beth.clj.lib.config :as config])
  (:use [beth.clj.lib.cljsc]
        [clojure.test]
        [midje.sweet]))

(def opts )

(background
 (around :facts
         (config/with-loaded-config
           {:cljs-dir.dev   "a"
            :cljs-file.dev  "b"
            :cljs-dir.prod  "c"
            :cljs-file.prod "d"
            :cljs-src       "z"}
           ?form)))

(deftest get-compiler-options-test
  (facts
   (get-compiler-options :dev)
   => (contains {:output-dir "a"
                 :output-to  "a/b"})
   
   (get-compiler-options :prod)
   => (contains {:output-dir "c"
                 :output-to  "c/d"
                 :optimizations :advanced})))

(deftest build-test
  (facts
   (build :dev)
   => ...built...
   (provided
    (get-compiler-options :dev) => ...opts...
    (cljs.closure/build "z" ...opts...) => ...built...)
   
   (build :prod)
   => ...built...
   (provided
    (get-compiler-options :prod) => ...opts...
    (cljs.closure/build "z" ...opts...) => ...built...)))
