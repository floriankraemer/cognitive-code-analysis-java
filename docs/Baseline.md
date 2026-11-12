# Baseline

A baseline is a saved result of `analyse`.
Later runs compare their metrics with it, so you can see whether a change made a method more or less complex.
The baseline is a snapshot of the metrics, not a list of accepted violations.

## Generating a baseline

Add `--generate-baseline` (`-g`) to an `analyse` run:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java -g
```

The run prints the analysis as usual and then the path of the new file, such as `Baseline file generated: .jcca/baseline/baseline-2026-11-12_21-35-02.json`.
The path is relative to the working directory.

The file is written to `.jcca/baseline` below the working directory, and its name carries the UTC time of the run.
An existing file is never replaced: when the name is taken, `-1`, `-2` and so on are appended.
The directory is created when it is missing.

## Using a baseline

Without an option, `analyse` uses the newest baseline in `.jcca/baseline`, and it says so before the results:

```text
Auto-detected latest baseline file: baseline-2026-11-12_21-35-02.json
```

The newest file is the one with the latest timestamp in its name, and the suffix breaks a tie.
Files whose name does not match `baseline-<timestamp>[-<n>].json`, or whose date is not valid, are ignored.

To compare with another file, pass it with `--baseline` (`-b`):

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java -b .jcca/baseline/baseline-2026-11-12_21-35-02.json
```

A file given with `-b` must exist, and it must be a valid baseline.
Only `analyse` reads baselines, and `churn` has no baseline option.

## Deltas

Each weight that differs from the baseline gets a delta, and so does the Cognitive Complexity score.

* The console shows the change in the cell below the value, as `Δ +1.099` in red when the value went up and `Δ -1.099` in green when it went down.
* The Markdown report shows `↑ 1.099` for an increase and `↓ 1.099` for a decrease.
* The HTML report shows the change as a badge, red for an increase and green for a decrease.
* The CSV report has a `… Weight Delta` column for each metric, which is empty for unchanged values.
* The JSON report has a `deltas` object per method, which is left out when there is no baseline.

Methods are matched by their class and method name.
A method that is new, or whose name changed, has no delta.

## File format

A baseline is a JSON object with four fields:

```json
{
  "version": "2.0",
  "createdAt": "2026-11-12 21:35:02",
  "configHash": "…",
  "metrics": { }
}
```

`metrics` has the same layout as the JSON report.
`configHash` identifies the configuration that produced the metrics.
The schema of the format is in [schemas/baseline.json](../schemas/baseline.json).

Files in the older format, which is a JSON report without the wrapper, are still accepted.
Such a file has no configuration hash, so no warning about the configuration is shown.

A file that is not JSON, that has an unsupported version or that misses a required field is rejected.
All problems of one file are listed in a single message, and `analyse` exits with status 1.

## Configuration changes

When the configuration of the current run differs from the one stored in the baseline, `analyse` prints a warning:

```text
Warning: Baseline config hash (…) does not match current config hash (…). Metrics comparison may not be accurate.
```

The warning does not stop the run.
A change of the weights, thresholds or scales changes the score, so the deltas of the score are then not comparable.
Generate a new baseline after a configuration change.

## Keeping baselines in version control

A baseline is most useful when the whole team compares against the same snapshot.
Commit the file that describes the state you want to be compared with, and leave `.jcca/baseline` out of `.gitignore`.
The repository of this tool does not ignore `.jcca/`, so its baselines can be committed as well.

The directory `.jcca.cache` is a different thing and should be ignored, see [Configuration](./Configuration.md#cache).

For a complete workflow in a pipeline, see [CI](./CI.md#baseline-workflow).
