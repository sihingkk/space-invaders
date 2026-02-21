(ns staffer.detection
  "Pattern matching / detection logic for finding invaders in a radar grid.")

(defn matching
  "Compares two strings character by character.
   Returns a vector of :match, :mismatch, or :padding (when radar char is \\_)."
  [s1 s2]
  (mapv (fn [a b]
          (cond
            (= \_ b) :padding
            (= a b)  :match
            :else    :mismatch))
        s1 s2))

(defn match-score
  "Compares an invader pattern against a radar window of the same dimensions.
   Ignores padding cells. Returns {:score 0-100 :visibility 0-100}."
  [pattern window]
  (let [results   (mapcat matching pattern window)
        visible   (remove #{:padding} results)
        total     (count results)
        vis-count (count visible)
        hits      (count (filter #{:match} visible))]
    {:score      (if (zero? vis-count) 0
                   (Math/round (double (* 100 (/ hits vis-count)))))
     :visibility (if (zero? total) 0
                   (Math/round (double (* 100 (/ vis-count total)))))}))

(defn pad-grid
  "Pads radar-grid with \\_ characters. Adds pad-h rows top/bottom
   and pad-w columns left/right."
  [radar-grid pad-h pad-w]
  (if (and (zero? pad-h) (zero? pad-w))
    radar-grid
    (let [grid-w    (apply max (map count radar-grid))
          padded-w  (+ grid-w (* 2 pad-w))
          blank-row (apply str (repeat padded-w \_))
          pad-left  (apply str (repeat pad-w \_))
          pad-right (fn [row]
                      (apply str (repeat (+ pad-w (- grid-w (count row))) \_)))
          pad-row   (fn [row] (str pad-left row (pad-right row)))]
      (vec
        (concat
          (repeat pad-h blank-row)
          (mapv pad-row radar-grid)
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
        grid-w (apply min (map count radar-grid))]
    (for [r (range (inc (- grid-h height)))
          c (range (inc (- grid-w width)))]
      {:row    r
       :col    c
       :window (extract-window radar-grid r c height width)})))

(defn- detect-invader
  "Returns a function that scores a window against the given invader pattern.
   The returned fn takes a window map {:row :col :window} and returns
   a match map with score, visibility, and adjusted coordinates."
  [pattern invader-name pad-h pad-w]
  (let [h (count pattern)
        w (count (first pattern))]
    (fn [{:keys [row col window]}]
      (let [{:keys [score visibility]} (match-score pattern window)]
        {:invader    invader-name
         :row        (- row pad-h)
         :col        (- col pad-w)
         :score      score
         :visibility visibility
         :height     h
         :width      w}))))

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
     {:invader    string  - invader name
      :row        int     - top-left row in original radar (can be negative)
      :col        int     - top-left col in original radar (can be negative)
      :score      int     - match quality on visible cells (100 = perfect)
      :visibility int     - percentage of invader cells on radar
      :height     int     - invader pattern height
      :width      int     - invader pattern width}"
  [radar-grid invaders threshold min-visibility]
  (let [max-h  (apply max (map (comp count :pattern) invaders))
        max-w  (apply max (map (comp count first :pattern) invaders))
        pad-h  (dec max-h)
        pad-w  (dec max-w)
        padded (pad-grid radar-grid pad-h pad-w)]
    (vec
      (mapcat
        (fn [{:keys [name pattern]}]
          (->> (sliding-windows padded (count pattern) (count (first pattern)))
               (map (detect-invader pattern name pad-h pad-w))
               (filter #(and (>= (:visibility %) min-visibility)
                             (>= (:score %) threshold)))))
        invaders))))
