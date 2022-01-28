(ns com.yetanalytics.flint.prefix
  (:require [clojure.zip :as zip]))

(defn- ast-zipper
  "Create a zipper out of the AST. Note that AST nodes of the form
   `[:keyword value]` are treated differently from generic colls."
  [ast]
  (zip/zipper (fn [x] (coll? x))
              (fn [x] (if (keyword? (first x)) (rest x) x))
              identity
              ast))

(defn validate-prefixes
  "Given a map (or set) of keyword IRI prefixes, along with a conformed
   AST, traverse the AST looking for prefixes that were not included in
   the prologue. Returns a vector of errors if prefix errors exist, `nil`
   otherwise; each error map includes a path of AST keywords."
  [prefixes ast]
  (loop [loc  (ast-zipper ast)
         errs []]
    (if-not (zip/end? loc)
      (let [anode (zip/node loc)]
        (if (and (->> anode vector?)
                 (->> anode count (= 2))
                 (->> anode first (= :prefix-iri))
                 (->> anode second namespace keyword (contains? prefixes) not))
          (recur (zip/next loc)
                 (conj errs {:iri (second anode)
                             :prefixes   prefixes
                             :path       (->> loc
                                              zip/path
                                              (filter #(-> % first keyword?))
                                              (mapv #(-> % first)))}))
          (recur (zip/next loc)
                 errs)))
      (not-empty errs))))
