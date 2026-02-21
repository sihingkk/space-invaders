(ns staffer.radar-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [staffer.radar :as radar]))

(defn- write-temp-file!
  "Writes `content` to a temp file and returns its path."
  [content]
  (let [f (java.io.File/createTempFile "radar-test" ".txt")]
    (.deleteOnExit f)
    (spit f content)
    (.getAbsolutePath f)))

(deftest parse-grid-basic
  (testing "parses a simple multi-line grid"
    (let [path (write-temp-file! "ooo\n---\nooo\n")
          grid (radar/parse-grid path)]
      (is (= ["ooo" "---" "ooo"] grid))))

  (testing "strips leading and trailing blank lines"
    (let [path (write-temp-file! "\n\nooo\n---\n\n")
          grid (radar/parse-grid path)]
      (is (= ["ooo" "---"] grid))))

  (testing "single-row grid"
    (let [path (write-temp-file! "o-o-o\n")
          grid (radar/parse-grid path)]
      (is (= ["o-o-o"] grid)))))

(deftest grid-height-test
  (testing "returns row count"
    (is (= 3 (radar/grid-height ["ooo" "---" "ooo"]))))

  (testing "returns 0 for empty grid"
    (is (= 0 (radar/grid-height [])))))

(deftest grid-width-test
  (testing "returns width of widest row"
    (is (= 5 (radar/grid-width ["ooo" "o----" "oo"]))))

  (testing "returns 0 for empty grid"
    (is (= 0 (radar/grid-width [])))))
