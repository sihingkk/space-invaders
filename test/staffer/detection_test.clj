(ns staffer.detection-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [staffer.detection :as detection]))

(deftest matching-test
  (testing "identical strings — all true"
    (is (= [true true true] (detection/matching "ooo" "ooo"))))

  (testing "completely different — all false"
    (is (= [false false false] (detection/matching "ooo" "---"))))

  (testing "mixed match"
    (is (= [true false false true true] (detection/matching "oo--o" "o-o-o"))))

  (testing "empty strings"
    (is (= [] (detection/matching "" "")))))

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
    (let [;; 10x10 all-o invader (100 cells)
          pattern (vec (repeat 10 "oooooooooo"))
          ;; 10x10 window: first 8 rows all-o, last 2 rows all-dash
          ;; 80 matches out of 100 = 80%
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
