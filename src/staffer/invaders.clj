(ns staffer.invaders
  "Loading and representing invader patterns."
  (:require
    [clojure.string :as str]
    [staffer.radar :as radar]))

(defn- filename->name
  "Derives an invader name from a file path.
   e.g. \"resources/invader_a.txt\" -> \"invader_a\""
  [path]
  (-> path
      (str/replace #".*/" "")
      (str/replace #"\.[^.]+$" "")))

(defn load-invader
  "Loads a single invader pattern from a file.
   Returns a map with :name (derived from filename) and :pattern (vector of strings)."
  [path]
  {:name    (filename->name path)
   :pattern (radar/parse-grid path)})

(defn load-invaders
  "Loads multiple invader patterns from a sequence of file paths.
   Returns a vector of invader maps."
  [paths]
  (mapv load-invader paths))
