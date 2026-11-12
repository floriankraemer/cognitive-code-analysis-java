# Configuration

## Choosing a configuration file

The tool reads its settings from a YAML file with the root key `cognitive`.
The file is merged over the bundled defaults, so it only needs to list the values you want to change.

The file is chosen in this order:

1. The file passed with `--config` / `-c`.
2. `jcca.yaml` in the working directory, if it exists.
3. The bundled defaults only.

```bash
java -jar cognitive-analysis.jar analyse <path-to-folder> --config <path-to-config-file>
```

## Merge rules

- Mappings are merged key by key.
  A `metrics` block that lists only some metrics keeps the bundled values of all others.
- Lists, such as `excludePatterns`, replace the bundled list.
- Unknown keys, wrong types and invalid values are errors.
  The message names the key path, and `analyse` exits with status 1.

## Settings

`metrics`, `excludeFilePatterns` and `excludePatterns` affect the analysis today.
The other settings (`scoreThreshold`, `show*`, `groupByClass`, `cache`, `customReporters`) are parsed and validated, and are applied from a later release.

```yaml
cognitive:
  excludeFilePatterns: []
  excludePatterns: []
  scoreThreshold: 0.5
  showOnlyMethodsExceedingThreshold: false
  showHalsteadComplexity: false
  showCyclomaticComplexity: false
  showUnderstandability: false
  groupByClass: true
  showDetailedCognitiveMetrics: true
  metrics:
    lineCount: {threshold: 60, scale: 25.0, enabled: true}
    argCount: {threshold: 4, scale: 1.0, enabled: true}
    returnCount: {threshold: 2, scale: 5.0, enabled: true}
    variableCount: {threshold: 4, scale: 5.0, enabled: true}
    propertyCallCount: {threshold: 4, scale: 15.0, enabled: true}
    ifCount: {threshold: 3, scale: 1.0, enabled: true}
    ifNestingLevel: {threshold: 1, scale: 1.0, enabled: true}
    elseCount: {threshold: 1, scale: 1.0, enabled: true}
  cache:
    enabled: true
    directory: ./.jcca.cache
  customReporters:
    cognitive: {}   # name: {class: com.example.MyReporter, jar: libs/my.jar}
    churn: {}
```

`scoreThreshold` must be greater than 0.
A metric `threshold` must not be negative, and its `scale` must be greater than 0.

## Excluding files and methods

Both lists hold Java regular expressions.
A pattern excludes an item when it is found anywhere in the item's text, so anchor it with `^` and `$` to match the whole text.
An invalid regular expression is a configuration error that names the pattern.

- `excludeFilePatterns` is matched against the absolute, normalized path of each Java file, written with `/` as the separator on every platform.
- `excludePatterns` is matched against the method name `com.example.Outer.Inner::method`, as described in [Analysed Methods](./Cognitive-Complexity-Analysis.md#analysed-methods).

```yaml
cognitive:
  excludeFilePatterns:
    - '/generated/'
    - 'Test\.java$'
  excludePatterns:
    - '::(get|set|is)[A-Z]'
    - '^com\.example\.legacy\.'
```

Use single quotes in YAML, so a backslash reaches the regular expression unchanged.

## Tuning the calculation

Each enabled metric adds `ln(1 + (value - threshold) / scale)` to the score of a method.
A value at or below its threshold adds nothing.
A disabled metric adds nothing and is left out of the score.
The score is the sum of the weights, rounded half-up to three decimals.

Feel free to adjust the values to match your opinion on what makes code complex.
The default values are a good starting point.
