(ns syrup.sparql.format.update-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cstr]
            [clojure.walk :as w]
            [syrup.sparql.format :as f]))

(deftest format-test
  (testing "format INSERT DATA"
    (is (= (cstr/join "\n" ["INSERT DATA {"
                            "    foo:x dc:title \"Title\" ."
                            "}"])
           (->> '[:insert-data-update
                  [[:insert-data [[:tvec [[:prefix-iri :foo/x]
                                          [:prefix-iri :dc/title]
                                          [:str-lit "Title"]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format DELETE DATA"
    (is (= (cstr/join "\n" ["DELETE DATA {"
                            "    GRAPH <http://example.org> {"
                            "        foo:x dc:title \"Title\" ."
                            "    }"
                            "}"])
           (->> '[:delete-data-update
                  [[:delete-data [[:quads [:graph
                                           [:iri "<http://example.org>"]
                                           [[:tvec [[:prefix-iri :foo/x]
                                                    [:prefix-iri :dc/title]
                                                    [:str-lit "Title"]]]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format DELETE WHERE"
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "}"])
           (->> '[:delete-where-update
                  [[:delete-where [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                                   [:tvec [[:var ?i] [:var ?j] [:var ?k]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["DELETE WHERE {"
                            "    ?x ?y ?z ."
                            "    ?i ?j ?k ."
                            "    ?s ?p ?o ."
                            "    GRAPH <http://example.org> {"
                            "        ?q ?r ?s ."
                            "    }"
                            "}"])
           (->> '[:delete-where-update
                  [[:delete-where
                    [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]
                     [:nform [:spo [[[:var ?i]
                                     [:po [[[:var ?j]
                                            [:o [[:var ?k]]]]]]]
                                    [[:var ?s]
                                     [:po [[[:var ?p]
                                            [:o [[:var ?o]]]]]]]]]]
                     [:quads [:graph
                              [:iri "<http://example.org>"]
                              [[:tvec [[:var ?q] [:var ?r] [:var ?s]]]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format DELETE...INSERT"
    (is (= (cstr/join "\n" ["INSERT {"
                            "    ?a ?b ?c ."
                            "}"
                            "USING NAMED <http://example.org/2>"
                            "WHERE {"
                            "    ?a ?b ?c ."
                            "}"])
           (->> '[:modify-update
                  [[:insert [[:tvec [[:var ?a] [:var ?b] [:var ?c]]]]]
                   [:using [:update/named-iri [:named [:iri "<http://example.org/2>"]]]]
                   [:where [:where-sub/where
                            [[:tvec [[:var ?a] [:var ?b] [:var ?c]]]]]]]]
                (w/postwalk f/format-ast))))
    (is (= (cstr/join "\n" ["WITH <http://example.org>"
                            "DELETE {"
                            "    ?x ?y ?z ."
                            "}"
                            "INSERT {"
                            "    ?a ?b ?c ."
                            "}"
                            "USING <http://example.org/2>"
                            "WHERE {"
                            "    ?x ?y ?z ."
                            "    ?a ?b ?c ."
                            "}"])
           (->> '[:modify-update
                  [[:with [:iri "<http://example.org>"]]
                   [:delete [[:tvec [[:var ?x] [:var ?y] [:var ?z]]]]]
                   [:insert [[:tvec [[:var ?a] [:var ?b] [:var ?c]]]]]
                   [:using [:update/iri [:iri "<http://example.org/2>"]]]
                   [:where [:where-sub/where
                            [[:nform
                              [:spo [[[:var ?x]
                                      [:po [[[:var ?y]
                                             [:o [[:var ?z]]]]]]]
                                     [[:var ?a]
                                      [:po [[[:var ?b]
                                             [:o [[:var ?c]]]]]]]]]]]]]]]
                (w/postwalk f/format-ast)))))
  (testing "format graph management updates"
    (testing "- LOAD"
      (is (= "LOAD <http://example.org/1>\nINTO <http://example.org/2>"
             (->> '[:load-update
                    [[:load [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:into [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast))))
      (is (= "LOAD SILENT <http://example.org/1>\nINTO <http://example.org/2>"
             (->> '[:load-update
                    [[:load-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:into [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast)))))
    (testing "- CLEAR"
      (is (= "CLEAR DEFAULT"
             (->> '[:clear-update
                    [[:clear [:update/kw :default]]]]
                  (w/postwalk f/format-ast))))
      (is (= "CLEAR NAMED"
             (->> '[:clear-update
                    [[:clear [:update/kw :named]]]]
                  (w/postwalk f/format-ast))))
      (is (= "CLEAR ALL"
             (->> '[:clear-update
                    [[:clear [:update/kw :all]]]]
                  (w/postwalk f/format-ast))))
      (is (= "CLEAR <http://example.org>"
             (->> '[:clear-update
                    [[:clear [:iri "<http://example.org>"]]]]
                  (w/postwalk f/format-ast))))
      (is (= "CLEAR SILENT <http://example.org>"
             (->> '[:clear-update
                    [[:clear-silent [:iri "<http://example.org>"]]]]
                  (w/postwalk f/format-ast))))
      (is (try (->> '[:clear-update
                      [[:clear [:update/kw :bad]]]]
                    (w/postwalk f/format-ast))
               (catch IllegalArgumentException _ true))))
    (testing "- DROP"
      (is (= "DROP DEFAULT"
             (->> '[:drop-update
                    [[:drop [:update/kw :default]]]]
                  (w/postwalk f/format-ast))))
      (is (= "DROP NAMED"
             (->> '[:drop-update
                    [[:drop [:update/kw :named]]]]
                  (w/postwalk f/format-ast))))
      (is (= "DROP ALL"
             (->> '[:drop-update
                    [[:drop [:update/kw :all]]]]
                  (w/postwalk f/format-ast))))
      (is (= "DROP <http://example.org>"
             (->> '[:drop-update
                    [[:drop [:iri "<http://example.org>"]]]]
                  (w/postwalk f/format-ast))))
      (is (= "DROP SILENT <http://example.org>"
             (->> '[:drop-update
                    [[:drop-silent [:iri "<http://example.org>"]]]]
                  (w/postwalk f/format-ast)))))
    (testing "- CREATE"
      (is (= "CREATE <http://example.org>"
             (->> '[:create-update
                    [[:create [:iri "<http://example.org>"]]]]
                  (w/postwalk f/format-ast))))
      (is (= "CREATE SILENT <http://example.org>"
             (->> '[:create-update
                    [[:create-silent [:iri "<http://example.org>"]]]]
                  (w/postwalk f/format-ast)))))
    (testing "- ADD"
      (is (= "ADD <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:add-update
                    [[:add [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast))))
      (is (= "ADD SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:add-update
                    [[:add-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast)))))
    (testing "- COPY"
      (is (= "COPY <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:copy-update
                    [[:copy [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast))))
      (is (= "COPY SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:copy-update
                    [[:copy-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast)))))
    (testing "- MOVE"
      (is (= "MOVE <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:move-update
                    [[:move [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast))))
      (is (= "MOVE SILENT <http://example.org/1>\nTO <http://example.org/2>"
             (->> '[:move-update
                    [[:move-silent [:update/named-graph [:iri "<http://example.org/1>"]]]
                     [:to [:update/named-graph [:iri "<http://example.org/2>"]]]]]
                  (w/postwalk f/format-ast)))))))
