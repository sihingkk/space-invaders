(ns staffer.detection
  "Pattern matching / detection logic for finding invaders in a radar grid.")

(defn- percent
  "Computes a rounded integer percentage. Returns 0 when denominator is zero."
  [numerator denominator]
  (if (zero? denominator) 0
    (Math/round (double (* 100 (/ numerator denominator))))))

(defn- padding-str
  "Returns a string of n underscore characters."
  [n]
  (apply str (repeat n \_)))

(defn matching
  "Compares two strings character by character.
   Returns a vector of :match, :mismatch, or :padding (when radar char is \\_)."
  [pattern-row radar-row]
  (mapv (fn [a b]
          (cond
            (= \_ b) :padding
            (= a b)  :match
            :else    :mismatch))
        pattern-row radar-row))

(defn match-score
  "Computes score and visibility from a flat seq of cell comparison results
   (:match, :mismatch, :padding). Returns {:score 0-100 :visibility 0-100}."
  [cell-results]
  (let [visible   (remove #{:padding} cell-results)
        total     (count cell-results)
        vis-count (count visible)
        hits      (count (filter #{:match} visible))]
    {:score      (percent hits vis-count)
     :visibility (percent vis-count total)}))

(defn pad-grid
  "Pads radar-grid with \\_ characters. Adds pad-h rows top/bottom
   and pad-w columns left/right."
  [radar-grid pad-h pad-w]
  (if (and (zero? pad-h) (zero? pad-w))
    radar-grid
    (let [grid-w    (apply max (map count radar-grid))
          padded-w  (+ grid-w (* 2 pad-w))
          blank-row (padding-str padded-w)
          pad-left  (padding-str pad-w)
          pad-row   (fn [row]
                      (str pad-left row (padding-str (+ pad-w (- grid-w (count row))))))]
      (vec
        (concat
          (repeat pad-h blank-row)
          (map pad-row radar-grid)
          (repeat pad-h blank-row))))))

(defn extract-window
  "Extracts a sub-grid from radar-grid at position (row, col)
   with the given height and width. Returns a vector of strings."
  [radar-grid row col height width]
  (->> (subvec radar-grid row (+ row height))
       (mapv #(subs % col (+ col width)))))

(defn sliding-windows
  "Returns a lazy seq of {:row r :col c :window [strings...]} for all positions
   where a pattern of height x width fits fully inside radar-grid."
  [radar-grid height width]
  (let [grid-h (count radar-grid)
        ;; Use min width to ensure extract-window stays in bounds for ragged grids
        grid-w (apply min (map count radar-grid))]
    (for [r (range (inc (- grid-h height)))
          c (range (inc (- grid-w width)))]
      {:row    r
       :col    c
       :window (extract-window radar-grid r c height width)})))

(defn- score-window
  "Scores a window against an invader pattern, adjusting coordinates for padding.
   Computes cell-results once and derives score/visibility from them."
  [pattern invader-name pad-h pad-w {:keys [row col window]}]
  (let [cell-results (mapv matching pattern window)
        {:keys [score visibility]} (match-score (flatten cell-results))]
    {:invader      invader-name
     :row          (- row pad-h)
     :col          (- col pad-w)
     :score        score
     :visibility   visibility
     :height       (count pattern)
     :width        (count (first pattern))
     :cell-results cell-results}))

(defn find-invaders
  "Scans radar-grid for occurrences of each invader in invaders.
   Pads the grid with _ to handle edge/boundary detection.
   Filters results by threshold (min match score on visible cells)
   and min-visibility (min percentage of invader on radar).

   Parameters:
     radar-grid     - vector of strings representing the radar
     invaders       - seq of maps {:name string :pattern [string ...]}
     threshold      - integer 0-100, minimum match score on visible cells
     min-visibility - integer 0-100, minimum % of invader on radar

   Returns a vector of match maps, each:
     {:invader      string  - invader name
      :row          int     - top-left row in original radar (can be negative)
      :col          int     - top-left col in original radar (can be negative)
      :score        int     - match quality on visible cells (100 = perfect)
      :visibility   int     - percentage of invader cells on radar
      :height       int     - invader pattern height
      :width        int     - invader pattern width
      :cell-results vector  - 2D vector of :match/:mismatch/:padding per cell}"
  [radar-grid invaders threshold min-visibility]
  (let [max-h  (apply max (map #(count (:pattern %)) invaders))
        max-w  (apply max (map #(count (first (:pattern %))) invaders))
        pad-h  (dec max-h)
        pad-w  (dec max-w)
        padded (pad-grid radar-grid pad-h pad-w)]
    (vec
      (mapcat
        (fn [{:keys [name pattern]}]
          (->> (sliding-windows padded (count pattern) (count (first pattern)))
               (map #(score-window pattern name pad-h pad-w %))
               (filter #(and (>= (:visibility %) min-visibility)
                             (>= (:score %) threshold)))))
        invaders))))
