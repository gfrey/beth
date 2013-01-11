(ns beth.clj.lib.config-test
  (:use [beth.clj.lib.config]
        [clojure.test]
        [midje.sweet]))


;; Example data for the tests to follow.
(def example-simple-cfg
  (str "# Comment\n"
       " \t  # Comment with trailing whitespace!\n"
       "key1=1\n"
       "key2= 2\n"
       "key3 =3\n"
       "key4 = 4\n"
       " key5=5\n"
       " key6=6 \n"))

(def example-extended-cfg
  (str "# Comment\n"
       "key1 -> 1\n"
       "key2 -> [1 \n"
       "         2 \n"
       " # Comment    \n"
       "         3]\n"
       "key3 -> #{:a :b :c}\n"
       "key4.a = 4a\n"
       "key4.b = 4b\n"))

(def cfg {:key1 1
          :key2 [1 2 3]
          :key3 #{:a :b :c}
          :key4.a "4a"
          :key4.b "4b"})

;; Make sure loading configurations works as intended. Loading should
;; remove comments, handle whitespace and support extended
;; configurations.
(deftest load-cfg-file-test
  (facts
   ;; Loading simple key-value pairs should remove all whitespace
   ;; properly.
   (load-config-file ...foo...) => (contains {:key1 "1"
                                              :key2 "2"
                                              :key3 "3"
                                              :key4 "4"
                                              :key5 "5"
                                              :key6 "6"})
   (provided
    (clojure.java.io/resource ...foo...) => ...bar...
    (slurp ...bar...) => example-simple-cfg)

   ;; Loading extended key-value pairs (hence the "->" assignment)
   ;; should result in proper value types (ints are ints, lists are
   ;; lists etc.)
   (load-config-file ...foo...) => (contains cfg)
   (provided
    (clojure.java.io/resource ...foo...) => ...bar...
    (slurp ...bar...) => example-extended-cfg)))


;; Make sure lookup of keys from the store works. Assuming that the
;; configuration store is a simple key value store (which is a valid
;; assumption ass the extended key-value pair loaded above is checked
;; to be equal to the used configuration).
(deftest lookup-cfg-option-test
  (facts
   (lookup cfg :key1) => 1
   (lookup cfg :key2) => [1 2 3]
   (lookup cfg :key3) => #{:a :b :c}
   (lookup cfg :key4 :a) => "4a"
   (lookup cfg :key4 :b) => "4b"
   (lookup cfg :key4 :c) => (throws Exception ":key4.c not configured!")
   (lookup cfg :missing) => (throws Exception ":missing not configured!")))
