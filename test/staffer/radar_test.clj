(ns staffer.radar-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [staffer.radar :as radar]
    [staffer.test-util :refer [write-temp-file!]]))

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

(deftest parse-grid-missing-file-test
  (testing "throws ex-info for missing file"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"File not found"
          (radar/parse-grid "nonexistent/path.txt")))))
