(ns staffer.detection
  "Pattern matching / detection logic for finding invaders in a radar grid.")

(defn matching
  "Compares two strings character by character.
   Returns a vector of booleans — true where characters match."
  [s1 s2]
  (mapv = s1 s2))

(defn match-score
  "Compares an invader `pattern` against a radar `window` of the same dimensions.
   Both are vectors of strings. Compares row by row using `matching`, then counts
   hits across all rows. Returns an integer score 0-100."
  [pattern window]
  (let [results (mapcat matching pattern window)
        total   (count results)
        hits    (count (filter true? results))]
    (if (zero? total)
      0
      (Math/round (double (* 100 (/ hits total)))))))

(defn find-invaders
  "Scans `radar-grid` for occurrences of each invader in `invaders`,
   using `threshold` as the minimum similarity score (0-100).

   Parameters:
     radar-grid - vector of strings representing the radar
     invaders   - seq of maps {:name string :pattern [string ...]}
     threshold  - integer 0-100, minimum match score

   Returns a vector of match maps, each:
     {:invader  string  - invader name
      :row      int     - top-left row of the match in the radar
      :col      int     - top-left column of the match in the radar
      :score    int     - similarity score (100 = perfect match)}"
  [_radar-grid _invaders _threshold]
  [])
