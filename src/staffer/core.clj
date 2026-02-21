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
  [["-f" "--format FORMAT" "Output format: table or color"
    :default "table"
    :validate [#(contains? #{"table" "color"} %) "Must be 'table' or 'color'"]]
   ["-t" "--threshold NUM" "Similarity threshold (0.0-1.0)"
    :default 0.8
    :parse-fn #(Double/parseDouble %)
    :validate [#(<= 0.0 % 1.0) "Must be between 0.0 and 1.0"]]
   ["-h" "--help" "Show this help"]])

(def usage-text
  "Usage: clojure -M:run [options] <invader-file>... <radar-file>

Detect space invaders in a radar sample.

The last positional argument is the radar file.
All preceding positional arguments are invader pattern files (at least one required).")

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
                        :options       {:format string :threshold double}}"
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

      :else
      {:action        :run
       :invader-paths (vec (butlast arguments))
       :radar-path    (last arguments)
       :options       (select-keys options [:format :threshold])})))

(defn -main
  "Application entry point."
  [& args]
  (let [{:keys [action message invader-paths radar-path options]}
        (validate-args args)]
    (case action
      :help  (do (println message) (System/exit 0))
      :error (do (println message) (System/exit 1))
      :run   (let [inv        (invaders/load-invaders invader-paths)
                   radar-grid (radar/parse-grid radar-path)
                   matches    (detection/find-invaders radar-grid inv (:threshold options))]
               (output/render (:format options) radar-grid inv matches)))))
