(ns syrup.sparql.spec.expr-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [syrup.sparql.spec.expr :as es]))

(deftest expr-conform-test
  (testing "Conforming expression"
    (testing "terminals"
      (is (s/valid? ::es/expr '?foo))
      (is (s/valid? ::es/expr "bar"))
      (is (s/valid? ::es/expr 2))
      (is (s/valid? ::es/expr false))
      (is (= [:expr-terminal [:var '?foo]]
             (s/conform ::es/expr '?foo)))
      (is (= [:expr-terminal [:dt-lit #inst "2022-01-19T22:20:49Z"]]
             (s/conform ::es/expr #inst "2022-01-19T22:20:49Z")))
      (is (= [:expr-terminal [:num-lit 100]]
             (s/conform ::es/expr 100)))
      (is (= [:expr-terminal [:bool-lit true]]
             (s/conform ::es/expr true)))
      (is (= [:expr-terminal [:str-lit "ok"]]
             (s/conform ::es/expr "ok"))))
    (is (= [:expr-branch {:op   'rand
                          :args []}]
           (s/conform ::es/expr '(rand))))
    (is (= [:expr-branch {:op   'bnode
                          :args []}]
           (s/conform ::es/expr '(bnode))))
    (is (= [:expr-branch {:op   'bnode
                          :args [[:expr-branch {:op   'rand
                                                :args []}]]}]
           (s/conform ::es/expr '(bnode (rand)))))
    (is (= [:expr-branch {:op   'count
                          :args [[:expr-terminal [:var '?foo]]]}]
           (s/conform ::es/expr '(count ?foo))))
    (is (= [:expr-branch {:op   'count
                          :args [[:expr-terminal [:wildcard '*]]]}]
           (s/conform ::es/expr '(count *))))
    (is (= [:expr-branch {:op   'bound
                          :args [[:expr-terminal [:var '?foo]]]}]
           (s/conform ::es/expr '(bound ?foo))))
    (is (= [:expr-branch {:op   'exists
                          :args [[:sub-where [[:tvec '[[:var ?s]
                                                       [:var ?p]
                                                       [:var ?o]]]]]]}]
           (s/conform ::es/expr '(exists [[?s ?p ?o]]))))
    (is (= [:expr-branch {:op   'contains
                          :args [[:expr-terminal [:str-lit "foo"]]
                                 [:expr-terminal [:str-lit "foobar"]]]}]
           (s/conform ::es/expr '(contains "foo" "foobar"))))
    (is (= [:expr-branch {:op   'regex
                          :args [[:expr-terminal [:var '?foo]]
                                 [:expr-terminal [:str-lit "bar"]]
                                 [:expr-terminal [:str-lit "i"]]]}]
           (s/conform ::es/expr '(regex ?foo "bar" "i"))))
    (is (= [:expr-branch {:op   'group-concat
                          :args [[:expr-terminal [:var '?foo]]
                                 [:expr-terminal [:kwarg {:k :separator
                                                          :v ";"}]]]}]
           (s/conform ::es/expr '(group-concat ?foo :separator ";"))))
    (is (= [:expr-branch {:op   'if
                          :args [[:expr-terminal [:bool-lit true]]
                                 [:expr-terminal [:num-lit 1]]
                                 [:expr-terminal [:num-lit 0]]]}]
           (s/conform ::es/expr '(if true 1 0))))
    (is (= [:expr-branch {:op   '+
                          :args [[:expr-terminal [:num-lit 1]]
                                 [:expr-terminal [:num-lit 2]]
                                 [:expr-branch {:op   '*
                                                :args [[:expr-terminal [:num-lit 3]]
                                                       [:expr-terminal [:num-lit 4]]]}]]}]
           (s/conform ::es/expr '(+ 1 2 (* 3 4)))))
    (is (= [:expr-branch {:op   [:prefix-iri :foo/my-custom-fn]
                          :args [[:expr-terminal [:num-lit 2]]
                                 [:expr-terminal [:num-lit 2]]]}]
           (s/conform ::es/expr '(:foo/my-custom-fn 2 2))))))

(deftest invalid-test
  (testing "Invalid values"
    (is (s/invalid? (s/conform ::es/expr '(rand 1))))
    (is (s/invalid? (s/conform ::es/expr '(not false true))))
    (is (s/invalid? (s/conform ::es/expr '(contains "foo"))))
    (is (s/invalid? (s/conform ::es/expr '(+))))))

(deftest expr-as-var-test
  (testing "expr-as-var spec"
    (is (= '[:expr-as-var
             [[:expr-branch {:op   +
                             :args ([:expr-terminal [:num-lit 2]]
                                    [:expr-terminal [:num-lit 2]])}]
              [:var ?foo]]]
           (s/conform ::es/expr-as-var '[(+ 2 2) ?foo])))
    (is (= '[:expr-as-var
             [[:expr-branch {:op   concat
                             :args ([:expr-terminal [:var ?G]]
                                    [:expr-terminal [:str-lit " "]]
                                    [:expr-terminal [:var ?S]])}]
              [:var ?name]]]
           (s/conform ::es/expr-as-var '[(concat ?G " " ?S) ?name])))))
