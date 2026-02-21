(ns staffer.invaders-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [staffer.invaders :as invaders]))

(defn- write-temp-file!
  "Writes `content` to a temp file with the given `name` and returns its path."
  [name content]
  (let [f (java.io.File/createTempFile name ".txt")]
    (.deleteOnExit f)
    (spit f content)
    (.getAbsolutePath f)))

(deftest load-invader-test
  (testing "loads pattern and derives name from filename"
    (let [path (write-temp-file! "test_invader" "---oo---\n--oooo--\n")
          inv  (invaders/load-invader path)]
      (is (vector? (:pattern inv)))
      (is (= ["---oo---" "--oooo--"] (:pattern inv)))
      ;; Name comes from temp file name — just check it's a non-empty string
      (is (string? (:name inv)))
      (is (pos? (count (:name inv)))))))

(deftest load-invaders-test
  (testing "loads multiple invaders"
    (let [p1   (write-temp-file! "inv_a" "ooo\n---\n")
          p2   (write-temp-file! "inv_b" "---\nooo\n")
          invs (invaders/load-invaders [p1 p2])]
      (is (= 2 (count invs)))
      (is (= ["ooo" "---"] (:pattern (first invs))))
      (is (= ["---" "ooo"] (:pattern (second invs)))))))

(deftest load-invader-from-resource-test
  (testing "loads the actual invader_a resource file"
    (let [inv (invaders/load-invader "resources/invader_a.txt")]
      (is (= "invader_a" (:name inv)))
      (is (= 8 (count (:pattern inv))))
      (is (= 11 (count (first (:pattern inv)))))))

  (testing "loads the actual invader_b resource file"
    (let [inv (invaders/load-invader "resources/invader_b.txt")]
      (is (= "invader_b" (:name inv)))
      (is (= 8 (count (:pattern inv))))
      (is (= 8 (count (first (:pattern inv))))))))
