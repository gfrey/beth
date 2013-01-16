;; # Beth Configuration
;; Configuration handling is done in this namespace.

(ns beth.clj.lib.config)


;; ## Configuration Handling
;; The following binding and macro are required to make the
;; configuration available to the different functions of the template
;; processing.

;; A binding for the configuration.
(def ^:dynamic *config* nil)

;; Macros to make the configuration available to the subsequent
;; methods. One with the already loaded configuration, the other with
;; the file to load.

(defmacro with-loaded-config [cfg & forms]
  `(binding [*config* ~cfg]
     ~@forms))

(defmacro with-config [filename & forms]
  `(binding [*config* (load-config-file ~filename)]
     ~@forms))

;; ## Configuration File Loading
;; The following functions are used to load configuration files.

(defn add-string-line
  "Add a string line (something of type 'key = value') to the
   accumulator."
  [acc k v]
  (-> acc
      (assoc :last nil)
      (assoc-in [:string k] v)))

(defn add-form-line
  "Add a form line (something of type 'key -> value') to the
   accumulator. Form line might be a multiline thing. That is why
   there are two ways to call this function."
  ([acc line]
   (let [k (:last acc)]
     (update-in acc [:form k] conj line)))
  ([acc k v]
   (-> acc
       (assoc :last k)
       (update-in [:forms] conj k)
       (assoc-in [:form k] [v]))))

(defn parse-lines
  "Parse all the lines of the configuration file. This function will
   be called in a reduce call."
  [acc line]
  (if-let [[_ k v] (re-matches #"^([A-Za-z0-9\.-]+)\s*=\s*(.+)$" line)]
    (add-string-line acc (keyword k) v)
    (if-let [[_ k v] (re-matches #"^([A-Za-z0-9\.-]+)\s*->\s*(.+)$" line)]
      (add-form-line acc (keyword k) v)
      (if (:last acc)
        (add-form-line acc line)
        (throw (Exception. "Syntax error reading config file!"))))))

(defn join-eval-data
  "The form lines must be evaluated to contain the correct data
   structures. This allows for usage of the known idioms in the
   configuration file."
  [acc]
  (binding [*read-eval* false]
    (for [k (:forms acc)]
      [k (->> (get-in acc [:form k])
              (clojure.string/join " ")
              (read-string))])))

(defn join-data
  "Join the key-value pairs of the accumulator into a hash-map for
   easy access."
  [acc]
  (into {} (concat (:string acc)
                   (join-eval-data acc))))

(defn load-config-file
  "Load the given file, parse the lines and put the results into a
   hash map that is returned."
  [filename]
  (let [lines (-> (clojure.java.io/resource filename)
                  (slurp)
                  (clojure.string/split #"\n"))]
    (->> lines
         (map clojure.string/trim)
         (remove empty?)
         (remove #(= (first %) \#))
         (reduce parse-lines {:last nil :string {} :form {} :forms []})
         (join-data))))


;; ## Configuration Usage
;; The following methods are required for accessing a configuraion
;; hash-map.

(defn lookup
  "Get the key from the configuration hash-map. If a mode is given add
   it to the key. Throws an exception if the key can not be found in
   the configuration."
  ([key]
     (if-let [result (clojure.core/get *config* key)]
       result
       (throw
        (Exception.
         (str key " not configured!")))))
  ([key mode]
     (let [keyname  (name key)
           modename (name mode)
           full-key (keyword (str keyname "." modename))]
       (lookup full-key))))