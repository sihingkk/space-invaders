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
      (is (= 0.8 (get-in result [:options :threshold])))))

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

  (testing "--format invalid produces error"
    (let [result (core/validate-args ["-f" "xml" "inv.txt" "radar.txt"])]
      (is (= :error (:action result))))))

(deftest validate-args-threshold-test
  (testing "--threshold 0.75 is accepted"
    (let [result (core/validate-args ["-t" "0.75" "inv.txt" "radar.txt"])]
      (is (= :run (:action result)))
      (is (= 0.75 (get-in result [:options :threshold])))))

  (testing "--threshold out of range produces error"
    (let [result (core/validate-args ["-t" "1.5" "inv.txt" "radar.txt"])]
      (is (= :error (:action result)))))

  (testing "--threshold negative produces error"
    (let [result (core/validate-args ["-t" "-0.1" "inv.txt" "radar.txt"])]
      (is (= :error (:action result))))))

(deftest validate-args-missing-test
  (testing "no arguments produces error"
    (let [result (core/validate-args [])]
      (is (= :error (:action result)))))

  (testing "single argument produces error (need at least 2)"
    (let [result (core/validate-args ["radar.txt"])]
      (is (= :error (:action result))))))
