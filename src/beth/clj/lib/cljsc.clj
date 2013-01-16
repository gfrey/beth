;; # ClojureScript Compilation
;; This namespace contains the function that will call the
;; ClojureScript compiler.

(ns beth.clj.lib.cljsc
  (:require [beth.clj.lib.config   :as config]
            [cljs.closure          :as cljsc]
            [clojure.tools.logging :as log]))


(defn get-compiler-options
  "Determine the required compiler options for Google's
   Closure-Compiler taking the environment and configuration into
   accout."
  [mode]
  (let [odir  (config/lookup :cljs-dir mode)
        ofile (config/lookup :cljs-file mode)
        opts  {:output-dir odir
               :output-to  (str odir "/" ofile)}]
    (if (= mode :dev)
      opts
      (assoc opts :optimizations :advanced))))

(defn build
  "The method calling the compiler with the required options."
  [mode]
  (let [build-opts (get-compiler-options mode)
        cljs-src   (config/lookup :cljs-src)]
    (log/info (format "Compiling ClojureScript (%s mode, sources in %s and options set to %s)" mode cljs-src build-opts))
    (cljsc/build cljs-src build-opts)))
  
