(ns staffer.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [staffer.core :as core]))

(deftest validate-args-help-test
  (testing "--help returns help action"
    (let [result (core/validate-args ["--help"])]
      (is (= :help (:action result)))
      (is (string? (:message result)))
      (is (.contains (:message result) "Options:")))))

(deftest validate-args-valid-test
  (testing "valid args with two invader files and one radar file"
    (let [result (core/validate-args ["inv1.txt" "inv2.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= ["inv1.txt" "inv2.txt"] (:invader-paths result)))
      (is (= "radar.txt" (:radar-path result)))
      (is (= "table" (get-in result [:options :format])))
      (is (= 80 (get-in result [:options :threshold])))
      (is (= 50 (get-in result [:options :visibility])))))

  (testing "valid args with single invader file"
    (let [result (core/validate-args ["inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= ["inv.txt"] (:invader-paths result)))
      (is (= "radar.txt" (:radar-path result))))))

(deftest validate-args-format-test
  (testing "--format color is accepted"
    (let [result (core/validate-args ["-f" "color" "inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= "color" (get-in result [:options :format])))))

  (testing "--format edn is accepted"
    (let [result (core/validate-args ["-f" "edn" "inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= "edn" (get-in result [:options :format])))))

  (testing "--format invalid produces error"
    (let [result (core/validate-args ["-f" "xml" "inv.txt" "radar.txt"])]
      (is (= :error (:action result))))))

(deftest validate-args-threshold-test
  (testing "--threshold 75 is accepted"
    (let [result (core/validate-args ["-t" "75" "inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= 75 (get-in result [:options :threshold])))))

  (testing "--threshold over 100 produces error"
    (let [result (core/validate-args ["-t" "101" "inv.txt" "radar.txt"])]
      (is (= :error (:action result)))))

  (testing "--threshold negative produces error"
    (let [result (core/validate-args ["-t" "-1" "inv.txt" "radar.txt"])]
      (is (= :error (:action result))))))

(deftest validate-args-visibility-test
  (testing "--visibility 60 is accepted"
    (let [result (core/validate-args ["-v" "60" "inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= 60 (get-in result [:options :visibility])))))

  (testing "--visibility over 100 produces error"
    (let [result (core/validate-args ["-v" "101" "inv.txt" "radar.txt"])]
      (is (= :error (:action result)))))

  (testing "--visibility negative produces error"
    (let [result (core/validate-args ["-v" "-1" "inv.txt" "radar.txt"])]
      (is (= :error (:action result))))))

(deftest validate-args-color-mode-test
  (testing "--color-mode region is default"
    (let [result (core/validate-args ["inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= "region" (get-in result [:options :color-mode])))))

  (testing "--color-mode score with -f color is accepted"
    (let [result (core/validate-args ["-f" "color" "-c" "score" "inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= "score" (get-in result [:options :color-mode])))))

  (testing "--color-mode diff with -f color is accepted"
    (let [result (core/validate-args ["-f" "color" "-c" "diff" "inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= "diff" (get-in result [:options :color-mode])))))

  (testing "--color-mode without -f color produces error"
    (let [result (core/validate-args ["-c" "score" "inv.txt" "radar.txt"])]
      (is (= :error (:action result)))
      (is (.contains (:message result) "--color-mode requires -f color"))))

  (testing "--color-mode invalid value produces error"
    (let [result (core/validate-args ["-f" "color" "-c" "neon" "inv.txt" "radar.txt"])]
      (is (= :error (:action result))))))

(deftest validate-args-missing-test
  (testing "no arguments produces error"
    (let [result (core/validate-args [])]
      (is (= :error (:action result)))))

  (testing "single argument produces error (need at least 2)"
    (let [result (core/validate-args ["radar.txt"])]
      (is (= :error (:action result))))))
