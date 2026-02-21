(ns staffer.core
  "Entry point: CLI argument parsing and orchestration."
  (:require
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [staffer.detection :as detection]
    [staffer.invaders :as invaders]
    [staffer.output :as output]
    [staffer.radar :as radar]))

(def cli-options
  [["-f" "--format FORMAT" "Output format: table, edn, or color"
    :default "table"
    :validate [#(contains? #{"table" "edn" "color"} %) "Must be 'table', 'edn', or 'color'"]]
   ["-t" "--threshold NUM" "Similarity threshold 0-100 (percentage)"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 0 % 100) "Must be between 0 and 100"]]
   ["-v" "--visibility NUM" "Minimum visibility 0-100 (% of invader on radar)"
    :default 50
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 0 % 100) "Must be between 0 and 100"]]
   ["-c" "--color-mode MODE" "Color mode: region, score, or diff (requires -f color)"
    :default "region"
    :validate [#(contains? #{"region" "score" "diff"} %)
               "Must be 'region', 'score', or 'diff'"]]
   ["-h" "--help" "Show this help"]])

(def usage-text
  "Usage: bb run [options] <invader-file>... <radar-file>

Detect space invaders in a radar sample.

The last positional argument is the radar file.
All preceding positional arguments are invader pattern files (at least one).
Shell globs work: bb run resources/invader_*.txt resources/radar_sample.txt

Examples:
  bb run resources/invader_*.txt resources/radar_sample.txt
  bb run -f color resources/invader_*.txt resources/radar_sample.txt
  bb run -f color -c score resources/invader_*.txt resources/radar_sample.txt
  bb run -f color -c diff resources/invader_*.txt resources/radar_sample.txt
  bb run -f edn -t 75 resources/invader_*.txt resources/radar_sample.txt
  bb run -v 100 resources/invader_*.txt resources/radar_sample.txt")

(defn- usage
  "Builds the full usage/help string."
  [options-summary]
  (str/join \newline
            [usage-text
             ""
             "Options:"
             options-summary]))

(defn- error-msg
  "Builds an error message string from a sequence of error strings."
  [errors]
  (str "The following errors occurred:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Parses and validates CLI arguments.
   Returns a map with one of:
     {:action :help    :message string}
     {:action :error   :message string}
     {:action :run     :invader-paths [string ...]
                        :radar-path    string
                         :options       {:format string :threshold int :visibility int}}"
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      {:action :help :message (usage summary)}

      errors
      {:action :error :message (error-msg errors)}

      (< (count arguments) 2)
      {:action :error
       :message (error-msg
                  [(str "Expected at least 2 positional arguments "
                        "(invader files + radar file), got "
                        (count arguments) ".")
                   ""
                   (usage summary)])}

      (and (not= "region" (:color-mode options))
           (not= "color" (:format options)))
      {:action :error
       :message (error-msg ["--color-mode requires -f color"])}

      :else
      {:action        :run
       :invader-paths (vec (butlast arguments))
       :radar-path    (last arguments)
       :options       (select-keys options
                                   [:format :threshold :visibility :color-mode])})))

(defn -main
  "Application entry point."
  [& args]
  (let [{:keys [action message invader-paths radar-path options]}
        (validate-args args)]
    (case action
      :help  (do (println message) (System/exit 0))
      :error (do (println message) (System/exit 1))
      :run   (let [{:keys [format threshold visibility color-mode]} options
                   inv        (invaders/load-invaders invader-paths)
                   radar-grid (radar/parse-grid radar-path)
                   matches    (detection/find-invaders radar-grid inv
                                                       threshold visibility)]
               (output/render format radar-grid matches
                              :color-mode color-mode)))))
