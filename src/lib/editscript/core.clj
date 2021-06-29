(ns lib.editscript.core
  (:require [editscript.core :refer [diff get-edits]]))

;; Workaround to make editscript works on subvector
;; https://ask.clojure.org/index.php/3319/reduce-kv-fails-on-subvec

(when-not (satisfies?   clojure.core.protocols/IKVReduce (subvec [1] 0))
  (extend-type clojure.lang.APersistentVector$SubVector
    clojure.core.protocols/IKVReduce
    (kv-reduce
      [subv f init]
      (let [cnt (.count subv)]
        (loop [k 0 ret init]
          (if (< k cnt)
            (let [val (.nth subv k)
                  ret (f ret k val)]
              (if (reduced? ret)
                @ret
                (recur (inc k) ret)))
            ret))))))

(defn diff->edits
  "Compute the transformations needed to turn a Clojure data structure a into a datastructre b, return a vector of transformations"
  [a b]
  (get-edits (diff a b)))
