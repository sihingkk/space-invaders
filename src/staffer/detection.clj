(ns staffer.detection
  "Pattern matching / detection logic for finding invaders in a radar grid.

   Stub implementation — returns no matches. Detection logic will be
   implemented in a subsequent iteration.")

(defn find-invaders
  "Scans `radar-grid` for occurrences of each invader in `invaders`,
   using `threshold` as the minimum similarity score (0.0-1.0).

   Parameters:
     radar-grid - vector of strings representing the radar
     invaders   - seq of maps {:name string :pattern [string ...]}
     threshold  - double in [0.0, 1.0], minimum match score

   Returns a vector of match maps, each:
     {:invader  string  - invader name
      :row      int     - top-left row of the match in the radar
      :col      int     - top-left column of the match in the radar
      :score    double  - similarity score (1.0 = perfect match)}"
  [_radar-grid _invaders _threshold]
  [])
