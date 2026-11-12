# Churn

Churn shows where complex code changes often.
A class that is both hard to understand and edited all the time is a likely source of bugs.
The `churn` command finds these hotspots.

## How it works

The command calculates the cognitive complexity score of every method, as the `analyse` command does.
The score of a class is the sum of the scores of its methods.
Then it asks git how often the file of the class changed.
The number of changes is counted once per file, however many classes the file declares.
Merge commits are not counted.

```text
churn = times changed × score
```

The classes are sorted by churn, highest first, and by class name when the churn is equal.
If two files declare a class with the same name, they are merged into one entry with the summed score and the higher number of changes.

## Usage

```bash
java -jar target/cognitive-analysis.jar churn src/main/java --since 2024-01-01
```

The path can be a file, a directory, or a comma-separated list of both, as for `analyse`.
The console shows the classes with a score and a churn above zero.

| Option | Default | Description |
| --- | --- | --- |
| `<path>` | required | Comma-separated Java and Kotlin files and directories; directories are searched recursively. |
| `-c`, `--config <file>` | `./jcca.yaml` if it exists | YAML configuration file, see [Configuration](./Configuration.md#choosing-a-configuration-file). |
| `-s`, `--since <date>` | `2000-01-01` | Count the changes since the start of this day, in the format `YYYY-MM-DD`. |
| `--vcs <type>` | `git` | Version control system. Only `git` is supported. |
| `-r`, `--report-type <type>` | console table | `json`, `csv`, `html`, `markdown`, `svg-treemap` or `svg`, in any letter case. `svg` is an alias of `svg-treemap`. A key of `customReporters.churn` is also accepted, see [Custom Reporters](./Custom-Reporters.md). |
| `-f`, `--report-file <file>` | none | The file the report is written to. |
| `--coverage-jacoco <file>` | none | JaCoCo XML coverage report, see [Coverage](./Coverage.md). |
| `--coverage-cobertura <file>` | none | Cobertura XML coverage report. |
| `--coverage-clover <file>` | none | Clover XML coverage report. |
| `-h`, `--help` | | Show the help and exit. |
| `-V`, `--version` | | Print the version and exit. |

The report type and the report file must be given together, and the directory of the report file must exist.
Only one coverage option can be given.

The command needs a `git` executable on the `PATH`, and the analysed files must be inside a git repository.
Otherwise it prints `git executable not found` or `Not a git repository: <directory>` and exits with status 1.

Changes are counted with `git rev-list --since=<date> --no-merges --count HEAD -- <file>`, so the history of the current branch is used.
The history must be complete, so a shallow clone can report fewer changes than it has.

## Reports

| Type | Content |
| --- | --- |
| `json` | `createdAt` and a `classes` object with `file`, `score`, `timesChanged`, `churn`, `coverage`, `riskChurn` and `riskLevel` per class. |
| `csv` | The columns `Class`, `File`, `Score`, `Churn` and `Times Changed`, followed by `Risk Churn`, `Coverage` and `Risk Level` when coverage is given. |
| `html` | A Bootstrap table of all classes. |
| `markdown` | A table of the classes with churn. |
| `svg-treemap` | A treemap of 1200 × 800 pixels. |

The area of a rectangle in the treemap is proportional to the churn of the class.
Its colour goes from green to red with the score, relative to the lowest and highest score in the report.
The tooltip shows the full class name, the churn and the score.
Classes without churn are not drawn.

`coverage`, `riskChurn` and `riskLevel` are `null` in JSON and `N/A` in the other outputs as long as no coverage data is read.

## Risk

When coverage data is available, the risk churn weights the churn with the missing coverage.

```text
riskChurn = times changed × score × (1 − coverage)
```

The risk level is the first rule that matches.

| Level | Churn | Coverage |
| --- | --- | --- |
| `CRITICAL` | above 30 | below 0.5 |
| `HIGH` | above 20 | below 0.7 |
| `MEDIUM` | above 10 | below 0.8 |
| `LOW` | anything else | |

## Classes without coverage data

A class that is absent from the coverage report, or present without any lines, has no coverage.
Its `coverage`, `riskChurn` and `riskLevel` are `null` in JSON and `N/A` in the other outputs, and it gets no risk level.
The tool warns instead of guessing when the report matches none or only some of the analysed classes, see [Coverage](./Coverage.md#warnings).
