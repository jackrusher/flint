(ns com.yetanalytics.flint.sparql
  (:require [clojure.java.io :as io])
  (:import [org.apache.jena.query QueryFactory]
           [org.apache.jena.update UpdateFactory]))

;; Ensure that outputs are indeed valid SPARQL (at least according to
;; Apache Jena).

(defmacro make-valid-sparql-tests [parse-fn dir-name]
  (let [files# (->> dir-name io/file file-seq (filter #(.isFile %)))
        tests# (map (fn [file#]
                      (let [fstr# (slurp file#)]
                        `(~parse-fn ~fstr#)))
                    files#)]
    `(do ~@tests#
         nil)))

(defn valid-queries-test
  []
  (make-valid-sparql-tests (fn [q] (QueryFactory/create q))
                           "dev-resources/test-fixtures/outputs/query/"))

(defn valid-updates-test
  []
  (make-valid-sparql-tests (fn [u] (UpdateFactory/create u))
                           "dev-resources/test-fixtures/outputs/update/"))

(defn valid-update-requests-test
  []
  (make-valid-sparql-tests (fn [u] (UpdateFactory/create u))
                           "dev-resources/test-fixtures/outputs/update-request/"))

(comment
  (valid-queries-test)
  (valid-updates-test)
  (valid-update-requests-test))

;; Query/Update writing sandbox

(comment
  (QueryFactory/create
   "SELECT (SUM(?x + ?y) AS ?sum) (str(?sum) AS ?z2)
    WHERE {
      ?x ?y ?z .
    }
    ORDER BY ?x")
  (QueryFactory/create
   "SELECT ((?z + SUM(?y + ?x)) AS ?sum) (?sum AS ?z2)
    WHERE {
      ?x ?y ?z .
    }
    GROUP BY ?z")
  (QueryFactory/create
   "SELECT ?q
    WHERE {
      ?x ?y ?z .
    }
    GROUP BY ?q")

  (QueryFactory/create
   "SELECT (COUNT(DISTINCT ?z) AS ?z)
    WHERE { ?x a <http://foo.org/Thing> }")

  (println
   (QueryFactory/create
    "SELECT ?x
     WHERE {
      { SELECT (?z AS ?x) WHERE { 
         SELECT ?x WHERE { ?x ?y ?z }
       } }
      { SELECT (?z AS ?x) WHERE { ?y a <http://foo.org/Thing2>. } }
    }"))

  (println
   (QueryFactory/create
    "SELECT ?x
     WHERE {
      SELECT (?z AS ?x) WHERE {
        SELECT ?x WHERE {?x a <http://foo.org/Thing1> . } }
    }"))
  )

(comment
  (UpdateFactory/create
   "PREFIX foo: <http://foo.org/>
    DELETE WHERE { ?x foo:bar ?y } ;
    
    PREFIX foo: <http://foo-bar.org/>
    PREFIX fii: <http://fii.org/>
    DELETE WHERE { ?x foo:bar ?y . ?z fii:fum ?w }")

  (UpdateFactory/create
   "BASE <http://foo.org/>
    PREFIX bar: <bar>
    DELETE WHERE { ?x bar:qux ?y . ?z <quu> ?w . } ;
    
    BASE <http://fii.org/>
    PREFIX bii: <bii>
    BASE <http://foe.org/>
    PREFIX boe: <boe>

    DELETE WHERE { ?x boe:goe ?y . ?z <joe> ?w . }")

  (UpdateFactory/create
   "BASE <http://foo.org/>
    DELETE WHERE { ?x <bar> ?y } ;
    
    BASE <http://baz.org/>
    DELETE WHERE { ?x <qux> ?y }"))
