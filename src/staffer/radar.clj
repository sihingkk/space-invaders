(ns staffer.radar
  "Radar grid parsing and dimension helpers."
  (:require
    [clojure.string :as str]))

(defn parse-grid
  "Reads a file at `path` and returns the grid as a vector of strings.
   Strips leading/trailing blank lines."
  [path]
  (->> (slurp path)
       str/split-lines
       (remove str/blank?)
       vec))

(defn grid-height
  "Returns the number of rows in `grid`."
  [grid]
  (count grid))

(defn grid-width
  "Returns the width of the widest row in `grid`."
  [grid]
  (if (empty? grid)
    0
    (apply max (map count grid))))
