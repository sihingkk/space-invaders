(ns staffer.radar
  "Radar grid parsing."
  (:require
    [clojure.string :as str])
  (:import
    (java.io File)))

(defn parse-grid
  "Reads a file at `path` and returns the grid as a vector of strings.
   Strips leading/trailing blank lines.
   Throws ex-info if file does not exist."
  [path]
  (when-not (.exists (File. ^String path))
    (throw (ex-info (str "File not found: " path) {:path path})))
  (->> (slurp path)
       str/split-lines
       (remove str/blank?)
       vec))
