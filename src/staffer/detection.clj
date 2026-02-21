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

(defn extract-window
  "Extracts a sub-grid from `radar-grid` at position (`row`, `col`)
   with the given `height` and `width`. Returns a vector of strings."
  [radar-grid row col height width]
  (->> (subvec radar-grid row (+ row height))
       (mapv #(subs % col (+ col width)))))

(defn sliding-windows
  "Returns a lazy seq of {:row r :col c :window [strings...]} for all positions
   where a pattern of `height` x `width` fits fully inside `radar-grid`."
  [radar-grid height width]
  (let [grid-h (count radar-grid)
        grid-w (apply min (map count radar-grid))]
    (for [r (range (inc (- grid-h height)))
          c (range (inc (- grid-w width)))]
      {:row    r
       :col    c
       :window (extract-window radar-grid r c height width)})))

(defn- detect-invader
  "Returns a function that scores a window against the given invader pattern.
   The returned fn takes a window map {:row :col :window} and returns
   a match map if score >= threshold, nil otherwise."
  [pattern invader-name threshold]
  (let [h (count pattern)
        w (count (first pattern))]
    (fn [{:keys [row col window]}]
      (let [score (match-score pattern window)]
        (when (>= score threshold)
          {:invader invader-name :row row :col col :score score
           :height h :width w})))))

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
      :score    int     - similarity score (100 = perfect match)
      :height   int     - invader pattern height
      :width    int     - invader pattern width}"
  [radar-grid invaders threshold]
  (vec
    (mapcat
      (fn [{:keys [name pattern]}]
        (->> (sliding-windows radar-grid (count pattern) (count (first pattern)))
             (keep (detect-invader pattern name threshold))))
      invaders)))
