# Reports

`analyse` writes a report with `--report-type` (`-r`) and `--report-file` (`-f`).
Both must be given together.
The type is `json`, `csv`, `html` or `markdown`, in any letter case.
The directory of the file must exist, and the file is always written as UTF-8.
Numbers use a dot and three decimals, whatever the locale is.

| Setting | json | csv | html | markdown |
| --- | --- | --- | --- | --- |
| `groupByClass` | always grouped | one row per method | yes | yes |
| `showOnlyMethodsExceedingThreshold` | all methods | all methods | yes | yes |
| `scoreThreshold` | no | no | highlights the score | note and filter |
| `showDetailedCognitiveMetrics`, `showHalsteadComplexity`, `showCyclomaticComplexity`, `showUnderstandability` | all | all | all | selects columns |

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
`|` and `\` are escaped, and weight changes are shown as `↑ 0.500` and `↓ 0.500`.
