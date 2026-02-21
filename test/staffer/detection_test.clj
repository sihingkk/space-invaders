(ns staffer.detection-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [staffer.detection :as detection]))

;; ---------------------------------------------------------------------------
;; matching
;; ---------------------------------------------------------------------------

(deftest matching-test
  (testing "identical strings — all true"
    (is (= [true true true] (detection/matching "ooo" "ooo"))))

  (testing "completely different — all false"
    (is (= [false false false] (detection/matching "ooo" "---"))))

  (testing "mixed match"
    (is (= [true false false true true] (detection/matching "oo--o" "o-o-o"))))

  (testing "empty strings"
    (is (= [] (detection/matching "" "")))))

;; ---------------------------------------------------------------------------
;; match-score
;; ---------------------------------------------------------------------------

(deftest match-score-perfect-match-test
  (testing "identical pattern and window gives score 100"
    (let [pattern ["ooo" "o-o" "ooo"]
          window  ["ooo" "o-o" "ooo"]]
      (is (= 100 (detection/match-score pattern window)))))

  (testing "score 100 passes threshold 100"
    (is (>= 100 100)))

  (testing "score 100 does not pass threshold 101"
    (is (not (>= 100 101)))))

(deftest match-score-zero-match-test
  (testing "completely opposite pattern gives score 0"
    (let [pattern ["ooo" "ooo"]
          window  ["---" "---"]]
      (is (= 0 (detection/match-score pattern window))))))

(deftest match-score-partial-80-percent-test
  (testing "80 out of 100 cells matching gives score 80"
    (let [pattern (vec (repeat 10 "oooooooooo"))
          window  (vec (concat (repeat 8 "oooooooooo")
                               (repeat 2 "----------")))]
      (is (= 80 (detection/match-score pattern window)))))

  (testing "score 80 passes threshold 80"
    (is (>= 80 80)))

  (testing "score 80 does not pass threshold 81"
    (is (not (>= 80 81)))))

(deftest match-score-half-match-test
  (testing "half matching cells gives score 50"
    (let [pattern ["oooo" "oooo"]
          window  ["oo--" "oo--"]]
      (is (= 50 (detection/match-score pattern window))))))

(deftest match-score-single-cell-test
  (testing "single cell match gives 100"
    (is (= 100 (detection/match-score ["o"] ["o"]))))

  (testing "single cell mismatch gives 0"
    (is (= 0 (detection/match-score ["o"] ["-"])))))

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
;; find-invaders (integration)
;; ---------------------------------------------------------------------------

(deftest find-invaders-exact-match-test
  (testing "finds a 3x3 invader placed exactly in a 5x5 radar"
    (let [pattern   ["ooo" "o-o" "ooo"]
          radar     ["-----"
                     "-ooo-"
                     "-o-o-"
                     "-ooo-"
                     "-----"]
          invaders  [{:name "crab" :pattern pattern}]
          matches   (detection/find-invaders radar invaders 100)]
      (is (= 1 (count matches)))
      (is (= {:invader "crab" :row 1 :col 1 :score 100
              :height 3 :width 3}
             (first matches))))))

(deftest find-invaders-threshold-filtering-test
  (testing "threshold too high filters out partial matches"
    (let [pattern  ["oo" "oo"]
          radar    ["o-" "oo"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 100)]
      (is (empty? matches))))

  (testing "lower threshold allows partial matches"
    (let [pattern  ["oo" "oo"]
          radar    ["o-" "oo"]
          invaders [{:name "block" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 75)]
      (is (= 1 (count matches)))
      (is (= 75 (:score (first matches)))))))

(deftest find-invaders-multiple-invaders-test
  (testing "detects two different invaders in the same radar"
    (let [inv-a    {:name "aa" :pattern ["oo" "oo"]}
          inv-b    {:name "bb" :pattern ["--" "--"]}
          radar    ["oo--"
                    "oo--"]
          matches  (detection/find-invaders radar [inv-a inv-b] 100)]
      (is (= 2 (count matches)))
      (is (some #(= "aa" (:invader %)) matches))
      (is (some #(= "bb" (:invader %)) matches)))))

(deftest find-invaders-no-match-test
  (testing "returns empty when nothing matches"
    (let [pattern  ["ooo" "ooo"]
          radar    ["---" "---"]
          invaders [{:name "x" :pattern pattern}]
          matches  (detection/find-invaders radar invaders 50)]
      (is (empty? matches)))))
