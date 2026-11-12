# Cognitive Code Analysis for Java and Kotlin

Cognitive Code Analysis measures how hard methods are to understand, in Java and Kotlin code.
It reports a cognitive complexity score per method, next to the metrics that explain it, and it can write the results as console tables, reports for CI systems, and baselines to compare against.
Java and Kotlin files are measured with the same metrics, so mixed projects can be analysed in one run.

## Requirements and build

The tool needs Java 21 to run and to build.
Build the executable jar with the Maven wrapper:

```bash
./mvnw -B verify
```

The jar is written to `target/cognitive-analysis.jar`.
The examples in this documentation use that path, and run from the root of this repository.

## Quick start

Analyse a directory and print one table per class:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java
```

Write the methods above the threshold to a SARIF file:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java -r sarif -f cognitive.sarif
```

Find the classes that are complex and change often:

```bash
java -jar target/cognitive-analysis.jar churn src/main/java --since 2024-01-01
```

Create a `jcca.yaml` with the defaults, which the tool then reads from the working directory:

```bash
java -jar target/cognitive-analysis.jar init
```

Run `java -jar target/cognitive-analysis.jar --help` for the list of commands.

## Commands

The tool has three commands, `analyse`, `churn` and `init`.
Each command accepts `-h`/`--help` and `-V`/`--version`.

### analyse

`analyse` calculates the metrics of the Java and Kotlin files in the given paths.
It prints the result as tables, or writes a report with `--report-type` and `--report-file`.

```text
analyse [-ghV] [--debug] [-b=<file>] [-c=<configFile>] [--coverage-clover=<file>]
        [--coverage-cobertura=<file>] [--coverage-jacoco=<file>] [-f=<reportFile>]
        [-r=<reportType>] [--sort-by=<field>] [--sort-order=<order>] <path>
```

| Option | Default | Description |
| --- | --- | --- |
| `<path>` | required | Comma-separated Java and Kotlin files and directories; directories are searched recursively. |
| `-c`, `--config <file>` | `./jcca.yaml` if it exists | YAML configuration file, see [Configuration](./docs/Configuration.md). |
| `-r`, `--report-type <type>` | console tables | One of `json`, `csv`, `html`, `markdown`, `checkstyle`, `junit`, `sarif`, `gitlab-codequality` or `github-actions`. A key of `customReporters.cognitive` is also accepted, see [Custom Reporters](./docs/Custom-Reporters.md). Requires `--report-file`. |
| `-f`, `--report-file <file>` | none | The file the report is written to. Requires `--report-type`. |
| `--sort-by <field>` | none | Sorts the methods by `score`, `halstead`, `cyclomatic`, `understandability`, `class`, `method`, `file`, a raw count such as `lineCount` or `ifCount`, or the weight of a count as `<count>Weight`, such as `ifCountWeight`. |
| `--sort-order <order>` | `asc` | `asc` or `desc`. |
| `-b`, `--baseline <file>` | newest file in `.jcca/baseline` | Baseline to compare against, see [Baseline](./docs/Baseline.md). |
| `-g`, `--generate-baseline` | off | Writes the metrics to `.jcca/baseline/baseline-<timestamp>.json` after the analysis. |
| `--coverage-jacoco <file>` | none | JaCoCo XML coverage report. |
| `--coverage-cobertura <file>` | none | Cobertura XML coverage report. |
| `--coverage-clover <file>` | none | Clover XML coverage report. |
| `--coverage-*` | | Only one coverage format can be given, see [Coverage](./docs/Coverage.md). |
| `--debug` | off | Prints the time of each stage and the statistics after the run. |
| `-h`, `--help` | | Shows the help and exits. |
| `-V`, `--version` | | Prints the version and exits. |

The sort fields are `score`, `halstead` (the Halstead volume), `cyclomatic`, `understandability`, `class`, `method`, `file`, the counts `lineCount`, `argCount`, `returnCount`, `variableCount`, `propertyCallCount`, `ifCount`, `ifNestingLevel` and `elseCount`, and their weights as `<count>Weight`.
Numbers are compared as numbers, and text without regard to letter case.
Methods with an equal value keep the order of class and method.
Within a class table the methods keep the sort order, and the class tables are always ordered by class name.
Reports follow the same order of methods.

The report type and the report file must be given together, and the directory of the report file must exist.
The issue formats (`checkstyle`, `junit`, `sarif`, `gitlab-codequality` and `github-actions`) list only the methods above `scoreThreshold`.
See [Reports](./docs/Reports.md) for the content of each format.

An `analyse` run exits with status 0 when violations are found, so a CI job needs its own gate, see [CI](./docs/CI.md).
A file that cannot be parsed is reported and skipped, and the analysis goes on.

#### Console output

The output starts with the configuration in use and the cache state, then one table per class.
A cell of the detail columns reads `count (weight)`, the score is red above `scoreThreshold`, and the Halstead values and risk levels are coloured from their thresholds.
Colours are used only on a terminal, and `NO_COLOR` switches them off.
The settings that choose the columns and the grouping are described in [Console output](./docs/Configuration.md#console-output).

Run on the sample fixture of this repository:

```text
Config: built-in
Cache: enabled

Found 1 files. Starting analysis.

Class: test.testcode.ClassToAnalyze
File: src/test/resources/fixtures/sample/ClassToAnalyze.java
┌────────────────────────┬────────┬───────────┬─────────┬───────────┬──────────┬───────┬────────────┬───────────┬────────────┐
│ Method Name            │ Lines  │ Arguments │ Returns │ Variables │ Property │  If   │ If Nesting │   Else    │ Cognitive  │
│                        │        │           │         │           │ Accesses │       │   Level    │           │ Complexity │
├────────────────────────┼────────┼───────────┼─────────┼───────────┼──────────┼───────┼────────────┼───────────┼────────────┤
│ simpleMethod           │ 3 (0)  │   0 (0)   │  0 (0)  │   0 (0)   │  0 (0)   │ 0 (0) │   0 (0)    │   0 (0)   │   0.000    │
│ methodWithTwoArguments │ 16 (0) │   2 (0)   │  0 (0)  │   0 (0)   │  0 (0)   │ 3 (0) │ 3 (1.099)  │ 3 (1.099) │   2.197    │
└────────────────────────┴────────┴───────────┴─────────┴───────────┴──────────┴───────┴────────────┴───────────┴────────────┘

Cognitive metrics calculated for src/test/resources/fixtures/sample
```

With `--debug`, the run also prints the time of each stage (`config`, `discovery`, `analysis`, `sorting` and `output`) and the counters `filesFound`, `filesAnalysed`, `filesFailed`, `methodsAnalysed`, `cacheHits` and `cacheMisses`.
On a terminal, a progress line shows how many files are analysed; it is written to the terminal only and never reaches redirected output.

#### Examples

Sort the most complex methods first:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java --sort-by score --sort-order desc
```

Write an HTML report:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java -r html -f cognitive.html
```

Analyse a mix of directories and single files:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java,src/test/resources/fixtures/kotlin/mixed
```

Compare with the newest baseline, and write a new one:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java -g
```

Add the line coverage of a JaCoCo report:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java --coverage-jacoco target/site/jacoco/jacoco.xml
```

### churn

`churn` multiplies the score of each class with the number of commits that changed its file, and ranks the classes by the result.
The number of changes comes from git.

```text
churn [-hV] [-c=<configFile>] [--coverage-clover=<file>] [--coverage-cobertura=<file>]
      [--coverage-jacoco=<file>] [-f=<reportFile>] [-r=<reportType>] [-s=<since>] [--vcs=<vcs>] <path>
```

| Option | Default | Description |
| --- | --- | --- |
| `<path>` | required | Comma-separated Java and Kotlin files and directories; directories are searched recursively. |
| `-c`, `--config <file>` | `./jcca.yaml` if it exists | YAML configuration file. |
| `-r`, `--report-type <type>` | console table | `json`, `csv`, `html`, `markdown`, `svg-treemap` or `svg`, or a key of `customReporters.churn`. Requires `--report-file`. |
| `-f`, `--report-file <file>` | none | The file the report is written to. Requires `--report-type`. |
| `-s`, `--since <date>` | `2000-01-01` | Counts the changes since this day, as `YYYY-MM-DD`. |
| `--vcs <type>` | `git` | Version control system. Only `git` is supported. |
| `--coverage-jacoco <file>` | none | JaCoCo XML coverage report, which adds the risk columns. |
| `--coverage-cobertura <file>` | none | Cobertura XML coverage report. |
| `--coverage-clover <file>` | none | Clover XML coverage report. |

The command needs `git` on the `PATH`, and the analysed files must be inside a git repository.
The full description, the report formats and the risk levels are in [Churn](./docs/Churn.md).

```bash
java -jar target/cognitive-analysis.jar churn src/main/java --since 2024-01-01 -r svg -f churn.svg
```

### init

`init` writes a configuration file with the bundled defaults, so you can start from a complete file.
In an interactive console it asks a few questions, and an empty answer keeps the default in brackets.

| Option | Default | Description |
| --- | --- | --- |
| `-p`, `--path <file>` | `./jcca.yaml` | Config file to create. Missing parent directories are created. |
| `--silent` | off | Skips the questions and writes the defaults. Questions are also skipped when the input is not a console. |
| `-f`, `--force` | off | Overwrites an existing config file. Without it, `init` refuses to replace a file and exits with status 1. |
| `-h`, `--help` | | Shows the help and exits. |
| `-V`, `--version` | | Prints the version and exits. |

```bash
java -jar target/cognitive-analysis.jar init --silent -p config/jcca.yaml
```

See [Configuration](./docs/Configuration.md#creating-a-config-file) for the questions.

## Kotlin

Kotlin files (`.kt`) are analysed with the same metrics, thresholds, reports, baselines and cache as Java files.
Kotlin scripts (`.kts`), such as `build.gradle.kts`, are not analysed.

```bash
java -jar target/cognitive-analysis.jar analyse src/test/resources/fixtures/kotlin/mixed
```

Kotlin functions are reported like Java methods.
Classes, data classes, enum classes, interfaces, objects and companion objects keep their names, such as `shop.OrderService` and `shop.OrderService.Companion`.
Top-level and extension functions belong to the JVM facade class of their file, such as `shop.OrderServiceKt` for `OrderService.kt`, or to the name given with `@file:JvmName`.

| Kotlin | Counted like the Java construct | Difference |
| --- | --- | --- |
| `if` statement or expression, `else if` | `if`, `else if` | |
| `when`, with or without subject | `switch` | Cyclomatic +1 per branch with conditions; `else` adds nothing. |
| `for (x in xs)`, `while`, `do`-`while` | enhanced `for`, `while`, `do`-`while` | |
| `try`/`catch`, `&&`, `\|\|` | `try`/`catch`, `&&`, `\|\|` | |
| `a ?: b` | no equivalent | Cyclomatic +1 only. |
| `?.`, `!!` | no equivalent | Not counted, except as Halstead operators. |
| lambdas and local functions | lambdas | Belong to the enclosing function and nest without an increment. |
| `return`, `return@label` | `return` | An expression body (`fun f() = x`) has no return. |
| `val`, `var`, destructuring, `it` | local variables | `it` counts when it is used. |
| `this.x`, `obj.x`, `obj?.x`, bare `x` | field access | |

The limits are that names are not resolved against a classpath, that `init` blocks, property accessors, primary constructors and enum entry bodies are not analysed, and that the Kotlin compiler makes the jar about 72 MB large.
The Kotlin parser adds about half a second to a run that finds Kotlin files.
The complete mapping, the function names and the limits are in [Kotlin](./docs/Cognitive-Complexity-Analysis.md#kotlin).

## Documentation

* [Cognitive Complexity Analysis](./docs/Cognitive-Complexity-Analysis.md): what is measured, how the score is built, and the Kotlin mapping.
* [Halstead, Cyclomatic and Understandability](./docs/Halstead-Complexity-Analysis.md): the other complexity measures in the output.
* [Configuration](./docs/Configuration.md): the configuration file, its settings, excludes, console output and cache.
* [Reports](./docs/Reports.md): the content of every report format.
* [Baseline](./docs/Baseline.md): generating, selecting and comparing baselines.
* [Coverage](./docs/Coverage.md): JaCoCo, Cobertura and Clover reports.
* [Churn](./docs/Churn.md): the churn command, its reports and risk levels.
* [Custom Reporters](./docs/Custom-Reporters.md): writing your own report format as a jar.
* [CI](./docs/CI.md): GitHub Actions and GitLab CI examples and a baseline workflow.

## Differences to the PHP version

This tool is a Java port of [cognitive-code-checker](https://github.com/Phauthentic/cognitive-code-analysis).
These behaviours differ on purpose.

* Cyclomatic complexity counts `&&` and `||`, which the PHP documentation describes but the PHP code does not count.
* Cyclomatic complexity counts each `case` entry with labels once, and never counts `default`.
* The `enabled` flag of a metric is honoured, and a disabled metric is left out of the score.
* A `metrics` block that lists only some metrics keeps the bundled thresholds of the others.
* A class that is missing from a coverage report has no coverage and no risk level, instead of counting as uncovered.
* `churn` asks git for the number of changes once per file, not once per method.
* `cache.directory` is honoured, and a disabled cache creates no directory.
* The cache key uses the content of the file, not its modification time, so touching a file does not invalidate its entry.
* `--debug` is a flag without a value.
* CSV reports have a header that matches their rows, and Markdown reports escape table characters.
* Issue reports keep the path of a file, and nothing is stripped from it.
* Halstead bugs and time are not reported, only volume, difficulty and effort.
* JaCoCo reports are accepted in addition to Cobertura and Clover.
* The configuration file is `jcca.yaml`, the cache is `.jcca.cache`, and baselines are stored in `.jcca/baseline`.
* Custom reporters are Java classes in jars, see [Custom Reporters](./docs/Custom-Reporters.md).
* Kotlin files are analysed in addition to Java.

## Resources

These pages and papers provide more information on cognitive limitations and readability and the impact on the business.

* **Cognitive Complexity**
  * [Cognitive Complexity Wikipedia](https://en.wikipedia.org/wiki/Cognitive_complexity)
  * [Cognitive Complexity and Its Effect on the Code](https://www.baeldung.com/java-cognitive-complexity) by Emanuel Trandafir.
  * [Human Cognitive Limitations.
    Broad, Consistent, Clinical Application of Physiological Principles Will Require Decision Support](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5822395/) by Alan H.
    Morris.
  * [Neural substrates of cognitive capacity limitations](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3131328/) by Timothy J.
    Buschman, Markus Siegel, Jefferson E.
    Roy, and Earl K.
    Miller.
  * [An Empirical Validation of Cognitive Complexity as a Measure of Source Code Understandability](https://arxiv.org/pdf/2007.12520) by Marvin Muñoz Barón, Marvin Wyrich, and Stefan Wagner.
* **Halstead Complexity**
  * [Halstead Complexity Measures](https://en.wikipedia.org/wiki/Halstead_complexity_measures)
* **Cyclomatic Complexity**
  * [Cyclomatic Complexity Wikipedia](https://en.wikipedia.org/wiki/Cyclomatic_complexity)
  * [Cyclomatic Complexity](https://www.ibm.com/docs/en/raa/6.1?topic=metrics-cyclomatic-complexity) by IBM.

## License

Copyright Florian Krämer

Licensed under the [GPL3 license](LICENSE).

### Third-party licenses

The executable jar bundles picocli, JavaParser, the Kotlin compiler and runtime, Jackson, SnakeYAML and Apache Commons, each under its own license, mostly the Apache License 2.0.
The jar lists them with their licenses in `META-INF/THIRD-PARTY-LICENSES.txt` and carries their notices in `META-INF/NOTICE`.
