(ns staffer.output-test
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [staffer.output :as output]))

(deftest format-table-empty-test
  (testing "empty matches prints no-invaders message"
    (let [out (with-out-str (output/format-table []))]
      (is (str/includes? out "No invaders detected.")))))

(deftest format-table-with-matches-test
  (testing "matches are printed as a table with headers"
    (let [matches [{:invader "invader_a" :row 5 :col 10 :score 87 :visibility 100}
                   {:invader "invader_b" :row 20 :col 30 :score 92 :visibility 60}]
          out     (with-out-str (output/format-table matches))
          lines   (str/split-lines out)]
      ;; Header line present
      (is (str/includes? (first lines) "INVADER"))
      (is (str/includes? (first lines) "ROW"))
      (is (str/includes? (first lines) "COL"))
      (is (str/includes? (first lines) "SCORE"))
      (is (str/includes? (first lines) "VISIBILITY"))
      ;; Both matches present with percentage display
      (is (str/includes? out "invader_a"))
      (is (str/includes? out "invader_b"))
      (is (str/includes? out "87%"))
      (is (str/includes? out "92%"))
      (is (str/includes? out "100%"))
      (is (str/includes? out "60%")))))

(deftest format-edn-empty-test
  (testing "empty matches prints empty vector"
    (let [out (with-out-str (output/format-edn []))]
      (is (= "[]\n" out)))))

(deftest format-edn-with-matches-test
  (testing "matches are printed as readable EDN"
    (let [matches [{:invader "invader_a" :row 5 :col 10 :score 87 :visibility 100}
                   {:invader "invader_b" :row 20 :col 30 :score 92 :visibility 60}]
          out     (with-out-str (output/format-edn matches))
          parsed  (edn/read-string out)]
      (is (vector? parsed))
      (is (= 2 (count parsed)))
      (is (= "invader_a" (:invader (first parsed))))
      (is (= 5 (:row (first parsed))))
      (is (= 10 (:col (first parsed))))))

  (testing "cell-results are excluded from EDN output"
    (let [matches [{:invader "inv" :row 0 :col 0 :score 90 :visibility 100
                    :height 2 :width 2
                    :cell-results [[:match :match] [:match :match]]}]
          out     (with-out-str (output/format-edn matches))
          parsed  (edn/read-string out)]
      (is (nil? (:cell-results (first parsed)))))))

(deftest format-color-empty-test
  (testing "empty matches prints plain radar grid"
    (let [grid ["ooo" "---" "ooo"]
          out  (with-out-str (output/format-color grid [] "region"))]
      (is (str/includes? out "ooo"))
      (is (str/includes? out "---"))
      ;; No ANSI escape codes
      (is (not (str/includes? out "\033["))))))

(deftest format-color-with-matches-test
  (testing "matches add ANSI color codes to output (region mode)"
    (let [grid    ["ooo" "---" "ooo"]
          matches [{:invader "inv" :row 0 :col 0 :score 90 :height 2 :width 2
                    :visibility 100
                    :cell-results [[:match :match] [:mismatch :mismatch]]}]
          out     (with-out-str (output/format-color grid matches "region"))]
      ;; Should contain ANSI escape codes
      (is (str/includes? out "\033["))
      ;; Should contain legend with invader name
      (is (str/includes? out "Legend:"))
      (is (str/includes? out "inv")))))

(deftest format-color-score-mode-test
  (let [make-match (fn [score]
                     [{:invader "inv" :row 0 :col 0 :score score
                       :height 2 :width 2 :visibility 100
                       :cell-results [[:match :match] [:match :mismatch]]}])
        grid ["ooo" "ooo"]]
    (doseq [[score color-code label]
            [[95 "\033[32m" "90%+ match"]
             [85 "\033[33m" "80-89% match"]
             [70 "\033[31m" "below 80% match"]]]
      (testing (str "score mode uses correct color for score " score)
        (let [out (with-out-str (output/format-color grid (make-match score) "score"))]
          (is (str/includes? out color-code))
          (is (str/includes? out label)))))))

(deftest format-color-diff-mode-test
  (testing "diff mode shows green for match and red for mismatch"
    (let [grid    ["o-" "oo"]
          matches [{:invader "inv" :row 0 :col 0 :score 75 :height 2 :width 2
                    :visibility 100
                    :cell-results [[:match :mismatch] [:match :match]]}]
          out     (with-out-str (output/format-color grid matches "diff"))]
      ;; Should have both green (match) and red (mismatch)
      (is (str/includes? out "\033[32m"))
      (is (str/includes? out "\033[31m"))
      ;; Legend
      (is (str/includes? out "match"))
      (is (str/includes? out "mismatch"))))

  (testing "diff mode skips padding cells (no color)"
    (let [grid    ["o" "o"]
          matches [{:invader "inv" :row 0 :col 0 :score 100 :height 2 :width 2
                    :visibility 50
                    :cell-results [[:match :padding] [:match :padding]]}]
          out     (with-out-str (output/format-color grid matches "diff"))]
      ;; Green for the match cells
      (is (str/includes? out "\033[32m"))
      ;; Only 1 char wide grid, padding cells are off-grid so no red
      (is (str/includes? out "o")))))

(deftest render-dispatches-test
  (testing "render with 'table' format calls format-table"
    (let [out (with-out-str (output/render "table" ["ooo"] []))]
      (is (str/includes? out "No invaders detected."))))

  (testing "render with 'edn' format calls format-edn"
    (let [out (with-out-str (output/render "edn" ["ooo"] []))]
      (is (= "[]\n" out))))

  (testing "render with 'color' format defaults to region mode"
    (let [out (with-out-str (output/render "color" ["ooo"] []))]
      (is (str/includes? out "ooo"))))

  (testing "render with 'color' format and score color-mode"
    (let [matches [{:invader "inv" :row 0 :col 0 :score 95 :height 1 :width 1
                    :visibility 100
                    :cell-results [[:match]]}]
          out     (with-out-str (output/render "color" ["o"] matches
                                               :color-mode "score"))]
      (is (str/includes? out "\033[32m"))
      (is (str/includes? out "90%+ match"))))

  (testing "render with 'color' format and diff color-mode"
    (let [matches [{:invader "inv" :row 0 :col 0 :score 100 :height 1 :width 1
                    :visibility 100
                    :cell-results [[:match]]}]
          out     (with-out-str (output/render "color" ["o"] matches
                                               :color-mode "diff"))]
      (is (str/includes? out "\033[32m"))
      (is (str/includes? out "match")))))
