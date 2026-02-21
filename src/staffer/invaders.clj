(ns staffer.invaders
  "Loading and representing invader patterns."
  (:require
    [clojure.string :as str]
    [staffer.radar :as radar])
  (:import
    (java.io File)))

(defn- filename->name
  "Derives an invader name from a file path.
   e.g. \"resources/invader_a.txt\" -> \"invader_a\""
  [path]
  (let [filename (.getName (File. ^String path))]
    (str/replace filename #"\.[^.]+$" "")))

(defn load-invader
  "Loads a single invader pattern from a file.
   Returns a map with :name (derived from filename) and :pattern (vector of strings).
   Throws ex-info if the pattern is empty."
  [path]
  (let [pattern (radar/parse-grid path)]
    (when (empty? pattern)
      (throw (ex-info (str "Empty invader pattern: " path) {:path path})))
    {:name    (filename->name path)
     :pattern pattern}))

(defn load-invaders
  "Loads multiple invader patterns from a sequence of file paths.
   Returns a vector of invader maps."
  [paths]
  (mapv load-invader paths))
