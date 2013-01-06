;; # Core Namespace
;; This namespace is just used for starting the application in
;; production mode.

(ns beth.clj.core
  (:require [beth.clj.server :as server]))
 
(defn -main [& args]
  (server/start-server :production))
