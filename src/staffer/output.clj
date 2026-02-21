(ns staffer.output
  "Result formatting: table, EDN, and ANSI color-coded radar display."
  (:require
    [clojure.pprint :as pprint]
    [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; ANSI color helpers
;; ---------------------------------------------------------------------------

(def ^:private ansi-colors
  "Distinct ANSI color codes for different invader types."
  ["\033[31m"    ; red
   "\033[32m"    ; green
   "\033[33m"    ; yellow
   "\033[36m"    ; cyan
   "\033[35m"    ; magenta
   "\033[34m"])  ; blue

(def ^:private ansi-reset "\033[0m")

(defn- invader-color-map
  "Assigns each unique invader name a distinct ANSI color code.
   Returns a map of invader-name -> ansi-escape-string."
  [matches]
  (let [names (distinct (map :invader matches))]
    (zipmap names (cycle ansi-colors))))

;; ---------------------------------------------------------------------------
;; Table format
;; ---------------------------------------------------------------------------

(defn format-table
  "Formats matches as a text table to stdout.
   Prints 'No invaders detected.' when matches is empty."
  [matches]
  (if (empty? matches)
    (println "No invaders detected.")
    (do
      (println (format "%-15s %5s %5s %6s %11s" "INVADER" "ROW" "COL" "SCORE" "VISIBILITY"))
      (println (str/join (repeat 46 "-")))
      (doseq [{:keys [invader row col score visibility]}
              (sort-by (juxt :row :col) matches)]
        (println (format "%-15s %5d %5d %4d%% %9d%%"
                         invader row col (int score) (int visibility)))))))

;; ---------------------------------------------------------------------------
;; EDN format
;; ---------------------------------------------------------------------------

(defn format-edn
  "Prints matches as pretty-printed EDN to stdout.
   Sorted by row, then column."
  [matches]
  (pprint/pprint (vec (sort-by (juxt :row :col) matches))))

;; ---------------------------------------------------------------------------
;; Color format — composable building blocks
;; ---------------------------------------------------------------------------

(defn- make-base-grid
  "Creates a 2D vector of [char nil] pairs from the radar grid.
   Each cell is [character color-or-nil], initially with no color."
  [radar-grid]
  (vec (for [row radar-grid]
         (vec (for [ch row]
                [ch nil])))))

(defn- on-grid?
  "Returns true if (row, col) is within the bounds of radar-grid."
  [radar-grid row col]
  (and (<= 0 row (dec (count radar-grid)))
       (<= 0 col (dec (count (get radar-grid row ""))))))

(defn- overlay-match
  "Overlays a single match onto a color grid using color-fn to determine
   the color for each cell. color-fn is called as
   (color-fn match pattern-row pattern-col) and should return a color
   string or nil (nil means skip this cell).

   Parameters:
     radar-grid - original radar grid (for bounds checking)
     color-fn   - (fn [match pattern-row pattern-col]) -> color-string | nil
     grid       - current 2D color grid (accumulator for reduce)
     match      - match map with :row :col :height :width (and any extra keys)"
  [radar-grid color-fn grid {:keys [row col height width] :as match}]
  (reduce
    (fn [g [pattern-row pattern-col]]
      (let [grid-row (+ row pattern-row)
            grid-col (+ col pattern-col)]
        (if (on-grid? radar-grid grid-row grid-col)
          (if-let [color (color-fn match pattern-row pattern-col)]
            (assoc-in g [grid-row grid-col 1] color)
            g)
          g)))
    grid
    (for [pattern-row (range height)
          pattern-col (range width)]
      [pattern-row pattern-col])))

(defn- build-color-grid
  "Builds a 2D vector of [char color-or-nil] for each cell in the radar.
   Uses color-fn to determine the color for each cell of each match.
   color-fn is called as (color-fn match pattern-row pattern-col)."
  [radar-grid matches color-fn]
  (reduce (partial overlay-match radar-grid color-fn)
          (make-base-grid radar-grid)
          matches))

(defn- print-color-grid
  "Prints a color grid (2D vector of [char color-or-nil] pairs) to stdout.
   Colored cells are wrapped with the ANSI color and reset codes."
  [color-grid]
  (doseq [row color-grid]
    (println
      (str/join
        (map (fn [[ch color]]
               (if color
                 (str color ch ansi-reset)
                 (str ch)))
             row)))))

(defn- region-color-fn
  "Creates a color-fn for region mode: uniform color per invader type.
   Returns a function (fn [match pattern-row pattern-col]) -> color-string."
  [color-map]
  (fn [match _pattern-row _pattern-col]
    (color-map (:invader match))))

(defn format-color
  "Prints the radar grid with ANSI colors highlighting detected invader regions.
   Each invader type gets a distinct color. Unmatched cells print normally."
  [radar-grid matches]
  (if (empty? matches)
    ;; No matches — print plain radar
    (doseq [row radar-grid]
      (println row))
    ;; Overlay colors
    (let [color-map  (invader-color-map matches)
          color-fn   (region-color-fn color-map)
          color-grid (build-color-grid radar-grid matches color-fn)]
      ;; Print legend
      (println "Legend:")
      (doseq [[inv-name color] color-map]
        (println (str "  " color inv-name ansi-reset)))
      (println)
      ;; Print colored grid
      (print-color-grid color-grid))))

;; ---------------------------------------------------------------------------
;; Dispatcher
;; ---------------------------------------------------------------------------

(defn render
  "Renders detection results in the given format.
   format-type is one of \"table\", \"edn\", or \"color\"."
  [format-type radar-grid matches]
  (case format-type
    "table" (format-table matches)
    "edn"   (format-edn matches)
    "color" (format-color radar-grid matches)))
