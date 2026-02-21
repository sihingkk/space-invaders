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
      (println (format "%-15s %5s %5s %6s" "INVADER" "ROW" "COL" "SCORE"))
      (println (str/join (repeat 34 "-")))
      (doseq [{:keys [invader row col score]} (sort-by (juxt :row :col) matches)]
        (println (format "%-15s %5d %5d %4d%%" invader row col (int score)))))))

;; ---------------------------------------------------------------------------
;; EDN format
;; ---------------------------------------------------------------------------

(defn format-edn
  "Prints matches as pretty-printed EDN to stdout.
   Sorted by row, then column."
  [matches]
  (pprint/pprint (vec (sort-by (juxt :row :col) matches))))

;; ---------------------------------------------------------------------------
;; Color format
;; ---------------------------------------------------------------------------

(defn- build-color-grid
  "Builds a 2D vector of [char color-or-nil] for each cell in the radar.
   Cells covered by a match get the invader's color; others get nil.
   Uses :height and :width from each match map for overlay dimensions."
  [radar-grid matches color-map]
  (let [grid-h    (count radar-grid)
        base-grid (vec (for [r (range grid-h)]
                         (vec (for [c (range (count (get radar-grid r)))]
                                [(get-in radar-grid [r c]) nil]))))
        overlay   (fn [grid {:keys [invader row col height width]}]
                    (let [color (color-map invader)]
                      (reduce
                        (fn [g [dr dc]]
                          (let [r (+ row dr)
                                c (+ col dc)]
                            (if (and (< -1 r grid-h)
                                     (< -1 c (count (get radar-grid r ""))))
                              (assoc-in g [r c 1] color)
                              g)))
                        grid
                        (for [dr (range height)
                              dc (range width)]
                          [dr dc]))))]
    (reduce overlay base-grid matches)))

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
          color-grid (build-color-grid radar-grid matches color-map)]
      ;; Print legend
      (println "Legend:")
      (doseq [[inv-name color] color-map]
        (println (str "  " color inv-name ansi-reset)))
      (println)
      ;; Print colored grid
      (doseq [row color-grid]
        (println
          (str/join
            (map (fn [[ch color]]
                   (if color
                     (str color ch ansi-reset)
                     (str ch)))
                 row)))))))

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
