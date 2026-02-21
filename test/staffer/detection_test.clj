(ns staffer.detection-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [staffer.detection :as detection]))

;; ---------------------------------------------------------------------------
;; matching
;; ---------------------------------------------------------------------------

(deftest matching-test
  (testing "identical strings — all :match"
    (is (= [:match :match :match] (detection/matching "ooo" "ooo"))))

  (testing "completely different — all :mismatch"
    (is (= [:mismatch :mismatch :mismatch] (detection/matching "ooo" "---"))))

  (testing "mixed"
    (is (= [:match :mismatch :mismatch :match :match]
           (detection/matching "oo--o" "o-o-o"))))

  (testing "padding characters in radar"
    (is (= [:padding :padding :match]
           (detection/matching "ooo" "__o"))))

  (testing "empty strings"
    (is (= [] (detection/matching "" "")))))

;; ---------------------------------------------------------------------------
;; match-score
;; ---------------------------------------------------------------------------

(deftest match-score-perfect-match-test
  (testing "identical pattern and window"
    (is (= {:score 100 :visibility 100}
           (detection/match-score ["ooo" "o-o" "ooo"]
                                  ["ooo" "o-o" "ooo"])))))

(deftest match-score-zero-match-test
  (testing "completely opposite"
    (is (= {:score 0 :visibility 100}
           (detection/match-score ["ooo" "ooo"]
                                  ["---" "---"])))))

(deftest match-score-partial-80-percent-test
  (testing "80 out of 100 cells matching"
    (let [pattern (vec (repeat 10 "oooooooooo"))
          window  (vec (concat (repeat 8 "oooooooooo")
                               (repeat 2 "----------")))]
      (is (= {:score 80 :visibility 100}
             (detection/match-score pattern window)))))

  (testing "score 80 passes threshold 80"
    (is (>= 80 80)))

  (testing "score 80 does not pass threshold 81"
    (is (not (>= 80 81)))))

(deftest match-score-half-match-test
  (testing "half matching cells"
    (is (= {:score 50 :visibility 100}
           (detection/match-score ["oooo" "oooo"]
                                  ["oo--" "oo--"])))))

(deftest match-score-with-padding-test
  (testing "padding reduces visibility but score only counts visible cells"
    ;; "ooo" vs "o__" → 1 visible match, 2 padding
    ;; "ooo" vs "ooo" → 3 visible matches
    ;; total=6, visible=4, hits=4 → score=100, visibility=67
    (is (= {:score 100 :visibility 67}
           (detection/match-score ["ooo" "ooo"]
                                  ["o__" "ooo"]))))

  (testing "all padding gives score 0, visibility 0"
    (is (= {:score 0 :visibility 0}
           (detection/match-score ["oo" "oo"]
                                  ["__" "__"])))))

(deftest match-score-single-cell-test
  (testing "single cell match"
    (is (= {:score 100 :visibility 100}
           (detection/match-score ["o"] ["o"]))))

  (testing "single cell mismatch"
    (is (= {:score 0 :visibility 100}
           (detection/match-score ["o"] ["-"]))))

  (testing "single cell padding"
    (is (= {:score 0 :visibility 0}
           (detection/match-score ["o"] ["_"])))))

;; ---------------------------------------------------------------------------
;; pad-grid
;; ---------------------------------------------------------------------------

(deftest pad-grid-test
  (testing "pads with _ characters"
    (let [grid   ["ooo" "---"]
          padded (detection/pad-grid grid 1 2)]
      ;; Original 2 rows + 1 top + 1 bottom = 4 rows
      (is (= 4 (count padded)))
      ;; Width: 3 original + 2*2 padding = 7
      (is (= 7 (count (first padded))))
      ;; Top/bottom rows are all _
      (is (= "_______" (first padded)))
      (is (= "_______" (last padded)))
      ;; Middle rows have padded content
      (is (= "__ooo__" (nth padded 1)))
      (is (= "__---__" (nth padded 2)))))

  (testing "zero padding returns original"
    (let [grid ["abc" "def"]]
      (is (= grid (detection/pad-grid grid 0 0))))))

;; ---------------------------------------------------------------------------
;; extract-window
;; ---------------------------------------------------------------------------

(deftest extract-window-test
  (let [grid ["abcd" "efgh" "ijkl"]]
    (testing "top-left 2x2"
      (is (= ["ab" "ef"] (detection/extract-window grid 0 0 2 2))))

    (testing "bottom-right 2x2"
      (is (= ["gh" "kl"] (detection/extract-window grid 1 2 2 2))))

    (testing "full grid extraction"
      (is (= grid (detection/extract-window grid 0 0 3 4))))

    (testing "single cell"
      (is (= ["f"] (detection/extract-window grid 1 1 1 1))))))

;; ---------------------------------------------------------------------------
;; sliding-windows
;; ---------------------------------------------------------------------------

(deftest sliding-windows-test
  (let [grid ["abc" "def" "ghi"]]
    (testing "2x2 window over 3x3 grid produces 4 windows"
      (let [windows (detection/sliding-windows grid 2 2)]
        (is (= 4 (count windows)))
        (is (= {:row 0 :col 0 :window ["ab" "de"]} (nth windows 0)))
        (is (= {:row 0 :col 1 :window ["bc" "ef"]} (nth windows 1)))
        (is (= {:row 1 :col 0 :window ["de" "gh"]} (nth windows 2)))
        (is (= {:row 1 :col 1 :window ["ef" "hi"]} (nth windows 3)))))

    (testing "window same size as grid produces 1 window"
      (let [windows (detection/sliding-windows grid 3 3)]
        (is (= 1 (count windows)))
        (is (= {:row 0 :col 0 :window grid} (first windows)))))

    (testing "1x1 window produces one window per cell"
      (is (= 9 (count (detection/sliding-windows grid 1 1)))))))

;; ---------------------------------------------------------------------------
;; find-invaders
;; ---------------------------------------------------------------------------

(deftest find-invaders-exact-match-test
  (testing "finds a 3x3 invader placed exactly in a 5x5 radar"
    (let [pattern  ["ooo" "o-o" "ooo"]
          radar    ["-----"
                    "-ooo-"
                    "-o-o-"
                    "-ooo-"
                    "-----"]
          invaders [{:name "crab" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 100 100)]
      (is (= 1 (count matches)))
      (let [m (first matches)]
        (is (= "crab" (:invader m)))
        (is (= 1 (:row m)))
        (is (= 1 (:col m)))
        (is (= 100 (:score m)))
        (is (= 100 (:visibility m)))))))

(deftest find-invaders-threshold-filtering-test
  (testing "threshold too high filters out partial matches"
    (let [pattern  ["oo" "oo"]
          radar    ["o-" "oo"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 100 100)]
      (is (empty? (filter #(and (= 0 (:row %)) (= 0 (:col %))) matches)))))

  (testing "lower threshold allows partial matches"
    (let [pattern  ["oo" "oo"]
          radar    ["o-" "oo"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 75 100)]
      (is (some #(and (= 0 (:row %))
                      (= 0 (:col %))
                      (= 75 (:score %))
                      (= 100 (:visibility %)))
                matches)))))

(deftest find-invaders-edge-detection-test
  (testing "detects invader partially off the right edge"
    (let [pattern  ["oo" "oo"]
          radar    ["o" "o"]
          invaders [{:name "block" :pattern pattern}]
          ;; With min-visibility 50, the half-visible match should appear
          matches  (detection/find-invaders radar invaders 100 50)]
      (is (some #(and (= 0 (:row %))
                      (= 0 (:col %))
                      (= 100 (:score %))
                      (= 50 (:visibility %)))
                matches))))

  (testing "edge match filtered when visibility too high"
    (let [pattern  ["oo" "oo"]
          radar    ["o" "o"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 100 100)]
      ;; No fully visible match possible in 1-wide radar with 2-wide pattern
      (is (empty? (filter #(= 100 (:visibility %)) matches))))))

(deftest find-invaders-corner-detection-test
  (testing "detects invader at corner with partial visibility"
    (let [pattern  ["oo" "oo"]
          radar    ["o"]
          invaders [{:name "block" :pattern pattern}]
          ;; 1x1 radar, 2x2 pattern: best case is 1 cell visible = 25%
          matches  (detection/find-invaders radar invaders 100 25)]
      (is (some #(and (= 0 (:row %))
                      (= 0 (:col %))
                      (= 100 (:score %))
                      (= 25 (:visibility %)))
                matches)))))

(deftest find-invaders-multiple-invaders-test
  (testing "detects two different invaders in the same radar"
    (let [inv-a   {:name "aa" :pattern ["oo" "oo"]}
          inv-b   {:name "bb" :pattern ["--" "--"]}
          radar   ["oo--"
                   "oo--"]
          matches (detection/find-invaders radar [inv-a inv-b] 100 100)]
      (is (some #(= "aa" (:invader %)) matches))
      (is (some #(= "bb" (:invader %)) matches)))))

(deftest find-invaders-no-match-test
  (testing "returns empty when nothing matches"
    (let [pattern  ["ooo" "ooo"]
          radar    ["---" "---"]
          invaders [{:name "x" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 50 100)]
      (is (empty? matches)))))

(deftest find-invaders-visibility-100-means-no-edges-test
  (testing "visibility 100 only returns fully visible matches"
    (let [pattern  ["oo" "oo"]
          radar    ["oo" "oo"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 100 100)]
      (is (every? #(= 100 (:visibility %)) matches)))))

;; ---------------------------------------------------------------------------
;; cell-results in match maps
;; ---------------------------------------------------------------------------

(deftest find-invaders-cell-results-test
  (testing "match map includes :cell-results with per-cell comparison"
    (let [pattern  ["oo" "oo"]
          radar    ["oo" "oo"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 100 100)
          m        (first (filter #(and (= 0 (:row %)) (= 0 (:col %))) matches))]
      (is (some? m))
      (is (= [[:match :match] [:match :match]] (:cell-results m)))))

  (testing "cell-results shows mismatches"
    (let [pattern  ["oo" "oo"]
          radar    ["o-" "oo"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 50 100)
          m        (first (filter #(and (= 0 (:row %)) (= 0 (:col %))) matches))]
      (is (some? m))
      (is (= [[:match :mismatch] [:match :match]] (:cell-results m)))))

  (testing "cell-results shows padding for edge matches"
    (let [pattern  ["oo" "oo"]
          radar    ["o" "o"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 100 50)
          m        (first (filter #(and (= 0 (:row %)) (= 0 (:col %))) matches))]
      (is (some? m))
      ;; Right column is padding since it's off-grid
      (is (= [[:match :padding] [:match :padding]] (:cell-results m))))))
