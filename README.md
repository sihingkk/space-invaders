# Staffer — Space Invader Radar Detector

Clojure application that detects known ASCII "space invader" patterns in a
noisy radar sample grid. Given one or more invader pattern files and a radar
scan file, it slides each pattern across the radar, scores every position by
similarity, and reports matches above a configurable threshold.

## Prerequisites

- Java 11+ (JDK)
- [Clojure CLI](https://clojure.org/guides/install_clojure) (deps.edn)
- [Babashka](https://github.com/babashka/babashka#installation) (optional but
  recommended as task runner)

## Setup

```sh
git clone <repo-url>
cd staffer
clojure -P              # download dependencies
```

## Usage

```
bb run [options] <invader-file>... <radar-file>
```

The last positional argument is the radar file. All preceding arguments are
invader pattern files (at least one). Shell globs work.

### Options

| Flag | Default | Description |
|------|---------|-------------|
| `-f, --format FORMAT` | `table` | Output format: `table`, `edn`, or `color` |
| `-t, --threshold NUM` | `80` | Minimum similarity score (0-100) |
| `-v, --visibility NUM` | `50` | Minimum % of invader visible on radar (0-100) |
| `-c, --color-mode MODE` | `region` | Color mode: `region`, `score`, or `diff` (requires `-f color`) |
| `-h, --help` | | Show help |

### Examples

```sh
# Table output (default)
bb run resources/invader_*.txt resources/radar_sample.txt

# Color output — region mode (each invader type gets a distinct color)
bb run -f color resources/invader_*.txt resources/radar_sample.txt

# Color output — score mode (green 90%+, yellow 80-89%, red below 80%)
bb run -f color -c score resources/invader_*.txt resources/radar_sample.txt

# Color output — diff mode (green=match, red=mismatch per cell)
bb run -f color -c diff resources/invader_*.txt resources/radar_sample.txt

# EDN output with lower threshold
bb run -f edn -t 75 resources/invader_*.txt resources/radar_sample.txt

# Only fully visible matches (no edge detections)
bb run -v 100 resources/invader_*.txt resources/radar_sample.txt
```

## Running Tests

```sh
bb test          # run all tests
bb lint          # lint with clj-kondo
bb ci            # lint + test
```

Or without Babashka:

```sh
clojure -M:test
clojure -M:lint
```

## Assumptions

- **Character set**: The radar and invader patterns use two characters: `o` and
  `-`. The padding character `_` is used internally for edge detection and does
  not appear in input files.
- **Noise model**: The radar contains noise as both false positives and false
  negatives — characters that differ from the true invader pattern.
- **No overlap**: Detected invaders are reported independently. The application
  does not attempt to resolve overlapping detections.
- **Edge detection**: Invaders may be partially off the edge of the radar. The
  `--visibility` flag controls the minimum percentage of the invader that must
  be on the radar to be reported (default 50%).
- **Threshold semantics**: The `--threshold` score is computed only over visible
  (non-padding) cells. A threshold of 80 means at least 80% of the visible
  cells must match the pattern.
- **Radar row width**: Rows in the radar file may vary slightly in width. The
  scanner uses the minimum row width for safe sliding window bounds.
