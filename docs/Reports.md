# Reports

`analyse` writes a report with `--report-type` (`-r`) and `--report-file` (`-f`).
Both must be given together.
The type is one of `json`, `csv`, `html`, `markdown`, `checkstyle`, `junit`, `sarif`, `gitlab-codequality` or `github-actions`, in any letter case.
The directory of the file must exist, and the file is always written as UTF-8.
Numbers use a dot and three decimals, whatever the locale is.
A value that is not a finite number is `N/A` in the text formats and `null` in JSON.
The file is written to a temporary file next to it and moved into place, so a failed write leaves an existing report intact.

| Setting | json | csv | html | markdown |
| --- | --- | --- | --- | --- |
| `groupByClass` | always grouped | one row per method | yes | yes |
| `showOnlyMethodsExceedingThreshold` | all methods | all methods | yes | yes |
| `scoreThreshold` | no | no | highlights the score | note and filter |
| `showDetailedCognitiveMetrics`, `showHalsteadComplexity`, `showCyclomaticComplexity`, `showUnderstandability` | all | all | all | selects columns |

The issue formats (`checkstyle`, `junit`, `sarif`, `gitlab-codequality`, `github-actions`) describe only the violations, so they ignore the table above and the grouping setting.
A violation is a method whose score is above `scoreThreshold`.

## JSON

```json
{
  "pkg.Class": {
    "file": "src/pkg/Class.java",
    "methods": {
      "method": { "class": "...", "method": "...", "file": "...", "line": 12, "lineCount": 3, "lineCountWeight": 0.0, "score": 1.5, "...": "..." }
    }
  }
}
```

Classes are sorted by name, methods keep the order of the analysis.
Weights, score and Halstead values are rounded to three decimals.
`deltas` holds the change of each weight against a baseline, and is left out when there is none.
When the same class name occurs in more than one file, for example in the default package, every occurrence is keyed `<class>@<file>`, with the file as shown in the reports, so nothing is lost.
Overloaded methods have distinct keys, as their parameter types are part of the name.

## CSV

One header row, then one row per method in the order of the analysis.
Per metric there are the columns `<Metric> Count`, `<Metric> Weight` and `<Metric> Weight Delta`, the last one empty without a baseline.

## HTML

A page that loads Bootstrap 5.3.3 from a CDN, so it needs a network connection to look as intended.
One table per class, or one table `All Methods (N total)` with a class column.
Scores above `scoreThreshold` are highlighted.
Weight changes against a baseline are badges, red for an increase and green for a decrease.

## Markdown

Tables per class, or one table of all methods.
In the grouped form a class that occurs in several files gets one section per file, titled `<class>@<file>`, as in the JSON keys.
`|`, `\`, backticks, `<` and `>` are escaped, line breaks become spaces, and weight changes are shown as `↑ 0.500` and `↓ 0.500`.

## Issue formats

Every issue format lists the methods above `scoreThreshold`, with the file as a path relative to the directory the command runs in, using `/` as the separator.
A file outside that directory is written as its absolute path, with `/` as the separator, and nothing is stripped from a path.
The severity is `error` when the score is at least twice the threshold, and `warning` otherwise.
The scores and the threshold in the messages have one decimal, rounded half up.

## Checkstyle

Checkstyle XML, version `8.0`, with one `file` element per file that has violations and one `error` element per method.
Each `error` has the line of the method, column `1`, the severity, the message `Method <name> has cognitive complexity <score> (threshold: <threshold>)` and the source `CognitiveComplexity`.
Without violations the report is an empty `checkstyle` element.

## JUnit

JUnit XML with one `testsuites` element that holds one `testsuite` named `Cognitive Complexity`.
Every method is a `testcase` with `classname`, `name` and `time="0"`.
A method above the threshold has one `failure` element with the message `Cognitive complexity <score> exceeds threshold <threshold>` and the type `CognitiveComplexity`.
The `errors` count is always `0`.

## SARIF

SARIF 2.1.0 for GitHub code scanning, with the schema `https://json.schemastore.org/sarif-2.1.0.json`.
The tool driver is `cognitive-analysis`, with its version from the build when the build provides one, and the information URI of the project.
The one rule is `cognitive-complexity`, with the level `warning` by default and the precision `high`.
Each result has the rule, the level, the message, the location with the path and the start line, and the fingerprint `partialFingerprints.primaryLocationLineHash`.
The fingerprint is the SHA-256 in hex of `<path>:<line>:<class>::<method>`, so a result keeps its identity while the lines above it move.

## GitLab Code Quality

A JSON array of issues in the CodeClimate format, which GitLab shows in merge requests.
Each issue has the description, the check name `cognitive-complexity`, the fingerprint (the same SHA-256 as in SARIF), the severity and the location with the path and the begin line.
The severity depends on the ratio of the score to the threshold: `blocker` from 3, `critical` from 2, `major` from 1.5, and `minor` above 1.
An empty array means that no method is above the threshold.

## GitHub Actions

One workflow command per violation, such as `::error file=src/pkg/Class.java,line=12::Method m has cognitive complexity 2.2 (threshold: 0.5)`.
The level is `error` or `warning` as in the other issue formats.
The message is escaped as the runner expects, so `%`, carriage returns and line feeds are encoded, and the file property also encodes `,` and `:`.
The file is empty when there is no violation, and otherwise ends with a line break.

Use the output of the `github-actions` format in a step that writes to the workflow log, so GitHub turns each line into an annotation.
