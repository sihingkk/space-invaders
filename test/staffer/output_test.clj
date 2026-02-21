(ns staffer.output-test
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [staffer.output :as output]))

(deftest format-table-empty-test
  (testing "empty matches prints no-invaders message"
    (let [out (with-out-str (output/format-table []))]
      (is (.contains out "No invaders detected.")))))

(deftest format-table-with-matches-test
  (testing "matches are printed as a table with headers"
    (let [matches [{:invader "invader_a" :row 5 :col 10 :score 0.87}
                   {:invader "invader_b" :row 20 :col 30 :score 0.92}]
          out     (with-out-str (output/format-table matches))
          lines   (str/split-lines out)]
      ;; Header line present
      (is (.contains (first lines) "INVADER"))
      (is (.contains (first lines) "ROW"))
      (is (.contains (first lines) "COL"))
      (is (.contains (first lines) "SCORE"))
      ;; Both matches present
      (is (.contains out "invader_a"))
      (is (.contains out "invader_b"))
      (is (.contains out "0.87"))
      (is (.contains out "0.92")))))

(deftest format-edn-empty-test
  (testing "empty matches prints empty vector"
    (let [out (with-out-str (output/format-edn []))]
      (is (= "[]\n" out)))))

(deftest format-edn-with-matches-test
  (testing "matches are printed as readable EDN"
    (let [matches [{:invader "invader_a" :row 5 :col 10 :score 0.87}
                   {:invader "invader_b" :row 20 :col 30 :score 0.92}]
          out     (with-out-str (output/format-edn matches))
          parsed  (edn/read-string out)]
      (is (vector? parsed))
      (is (= 2 (count parsed)))
      (is (= "invader_a" (:invader (first parsed))))
      (is (= 5 (:row (first parsed))))
      (is (= 10 (:col (first parsed)))))))

(deftest format-color-empty-test
  (testing "empty matches prints plain radar grid"
    (let [grid ["ooo" "---" "ooo"]
          out  (with-out-str (output/format-color grid [] []))]
      (is (.contains out "ooo"))
      (is (.contains out "---"))
      ;; No ANSI escape codes
      (is (not (.contains out "\033["))))))

(deftest format-color-with-matches-test
  (testing "matches add ANSI color codes to output"
    (let [grid     ["ooo" "---" "ooo"]
          invaders [{:name "inv" :pattern ["oo" "oo"]}]
          matches  [{:invader "inv" :row 0 :col 0 :score 0.9}]
          out      (with-out-str (output/format-color grid matches invaders))]
      ;; Should contain ANSI escape codes
      (is (.contains out "\033["))
      ;; Should contain legend
      (is (.contains out "Legend:")))))

(deftest render-dispatches-test
  (testing "render with 'table' format calls format-table"
    (let [out (with-out-str (output/render "table" ["ooo"] [] []))]
      (is (.contains out "No invaders detected."))))

  (testing "render with 'edn' format calls format-edn"
    (let [out (with-out-str (output/render "edn" ["ooo"] [] []))]
      (is (= "[]\n" out))))

  (testing "render with 'color' format calls format-color"
    (let [out (with-out-str (output/render "color" ["ooo"] [] []))]
      (is (.contains out "ooo")))))
