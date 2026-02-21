# Staffer — Space Invader Radar Detector

Clojure application that detects known ASCII "space invader" patterns in a
noisy radar sample grid. Given one or more invader pattern files and a radar
scan file, it slides each pattern across the radar, scores every position by
similarity, and reports matches above a configurable threshold.

## Preview

![demo](https://github.com/user-attachments/assets/94ac0671-2830-4ac6-8185-3a26c25c766c)


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
- **Delivery of data**: I just enforced file structure and delivery method, for
  real life scenario, most likely we would like to have some streaming solution
  and a reasonable time of response.

## Potential improvements:

#### Option A: Levenshtein / Edit distance
Measures the minimum number of insertions, deletions, and substitutions to transform one string into another. This captures structural similarity — if a pattern is slightly shifted or has characters inserted/deleted, Levenshtein handles it gracefully.
Problem: Our invaders are 2D grids, not 1D strings. Levenshtein works on sequences. You'd need to either:
1. Flatten to 1D (loses spatial structure)
2. Apply per-row and aggregate (misses vertical shifts)
3. Use a 2D edit distance (much more complex, no standard library)
Bigger problem: The detection model assumes the pattern is at a fixed grid position — the sliding window already handles displacement. Within a given window, the pattern and radar are already aligned cell-by-cell. There are no insertions or deletions to detect — only noise flips (o ↔ -). Levenshtein collapses to Hamming distance in this case (substitution-only), so it would give the same result with more computational overhead.

#### Option B: Jaccard similarity
Measures set overlap: |A ∩ B| / |A ∪ B|. Would treat the pattern and radar window as sets of character positions.
Problem: Loses positional information. Two completely different patterns with the same count of o and - characters would score identically.

#### Option C: Cosine similarity
Treats the patterns as vectors (e.g., o=1, -=0) and computes the cosine of the angle between them.
Problem: Similar to Jaccard — it measures overall character distribution similarity, not positional accuracy. A radar window with the right number of os in the wrong positions would score well.

#### Option D: Normalized Hamming distance
This is... exactly what we already have. score = 100 * (1 - hamming_distance / total_cells).
