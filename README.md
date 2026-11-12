# Cognitive Complexity Analysis

Cognitive Code Analysis is an approach to understanding and improving code by focusing on how human cognition interacts with code. It emphasizes making code more readable, understandable, and maintainable by considering the cognitive processes of the developers who write and work with the code.

> "Human short-term or working memory was estimated to be limited to 7 ± 2 variables in the 1950s. A more current estimate is 4 ± 1 constructs. Decision quality generally becomes degraded once this limit of four constructs is exceeded."

[Source: Human Cognitive Limitations. Broad, Consistent, Clinical Application of Physiological Principles Will Require Decision Support](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5822395/)

## Running it

Build the executable jar (requires Java 21):

```bash
./mvnw -B verify
```

Analyse a directory and print the result tables to the console:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java
```

The path can also be a single file or a comma-separated list of files and directories:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java,tools/Generator.java
```

Directories are searched recursively for `.java` files, skipping hidden directories.
A file that cannot be parsed is reported and skipped, and the analysis goes on.

The console output starts with the configuration in use and the cache state, then one table per class:

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
```

A cell of the detail columns reads `count (weight)`, with the weight shown with up to three decimals.
The score is red above `scoreThreshold` and green otherwise, Halstead values and risk levels are yellow or red from their thresholds.
Colors are used only on a terminal, and `NO_COLOR` switches them off.
The settings that choose the columns and the grouping are described in [Configuration](./docs/Configuration.md#console-output).

Order the methods with `--sort-by <field>` and `--sort-order asc|desc` (default `asc`):

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java --sort-by score --sort-order desc
```

The fields are `score`, `halstead`, `cyclomatic`, `understandability`, `class`, `method`, `file`, the raw counts `lineCount`, `argCount`, `returnCount`, `variableCount`, `propertyCallCount`, `ifCount`, `ifNestingLevel`, `elseCount`, and the weight of each count as `<count>Weight`, for example `ifCountWeight`.
Numbers are compared as numbers and text without regard to letter case; methods with an equal value stay in order of class and method.
Reports follow the same order.
Within a class table the methods keep the sort order, and the class tables themselves are always ordered by class name.

Use `--debug` to print the time of each stage (`config`, `discovery`, `analysis`, `sorting`, `output`) and the counters `filesFound`, `filesAnalysed`, `filesFailed`, `methodsAnalysed`, `cacheHits` and `cacheMisses` after the run.
On a terminal, a progress line on the error stream shows how many files are analysed.

Write an HTML, CSV or JSON report instead:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java --report-type html --report-file report.html
```

Create a `jcca.yaml` with the bundled defaults in the working directory.
The command asks a few questions, and an empty answer keeps the default in brackets:

```bash
java -jar target/cognitive-analysis.jar init
```

Use `--silent` to skip the questions, `--path` (`-p`) to choose another file, and `--force` (`-f`) to overwrite an existing file.
Questions are asked only in an interactive console, so piped input always takes the defaults.
See [Configuration](./docs/Configuration.md#creating-a-config-file) for details.

Find the classes that are complex and change often with the `churn` command, which asks git for the number of changes per file:

```bash
java -jar target/cognitive-analysis.jar churn src/main/java --since 2024-01-01
```

See [Churn](./docs/Churn.md) for the options and report formats.

Run `java -jar target/cognitive-analysis.jar --help` for all options.

## Documentation

* [Cognitive Complexity Analysis](./docs/Cognitive-Complexity-Analysis.md#cognitive-complexity-analysis)
    * [Why bother?](./docs/Cognitive-Complexity-Analysis.md#why-bother)
    * [What is the difference to Cyclomatic Complexity?](./docs/Cognitive-Complexity-Analysis.md#what-is-the-difference-to-cyclomatic-complexity)
    * [How is Cognitive Complexity calculated?](./docs/Cognitive-Complexity-Analysis.md#how-is-cognitive-complexity-calculated)
    * [Metrics Collected](./docs/Cognitive-Complexity-Analysis.md#metrics-collected)
    * [Result Interpretation](./docs/Cognitive-Complexity-Analysis.md#result-interpretation)
    * [Configuration](./docs/Configuration.md#configuration)
        * [Tuning the calculation](./docs/Configuration.md#tuning-the-calculation)
    * [Example](#example)

* [Churn](./docs/Churn.md)

* [Halstead Complexity Analysis](./docs/Halstead-Complexity-Analysis.md)

## Resources

These pages and papers provide more information on cognitive limitations and readability and the impact on the business.

* **Cognitive Complexity**
  * [Cognitive Complexity Wikipedia](https://en.wikipedia.org/wiki/Cognitive_complexity)
  * [Cognitive Complexity and Its Effect on the Code](https://www.baeldung.com/java-cognitive-complexity) by Emanuel Trandafir.
  * [Human Cognitive Limitations. Broad, Consistent, Clinical Application of Physiological Principles Will Require Decision Support](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5822395/) by Alan H. Morris.
  * [The Magical Number 4 in Short-Term Memory: A Reconsideration of Mental Storage Capacity](https://www.researchgate.net/publication/11830840_The_Magical_Number_4_in_Short-Term_Memory_A_Reconsideration_of_Mental_Storage_Capacity) by Nelson Cowan
  * [Neural substrates of cognitive capacity limitations](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3131328/) by Timothy J. Buschman,a,1 Markus Siegel,a,b Jefferson E. Roy, and Earl K. Millera.
  * [Code Readability Testing, an Empirical Study](https://www.researchgate.net/publication/299412540_Code_Readability_Testing_an_Empirical_Study) by Todd Sedano.
  * [An Empirical Validation of Cognitive Complexity as a Measure of Source Code Understandability](https://arxiv.org/pdf/2007.12520) by Marvin Muñoz Barón, Marvin Wyrich, and Stefan Wagner.
* **Halstead Complexity**
  * [Halstead Complexity Measures](https://en.wikipedia.org/wiki/Halstead_complexity_measures)
* **Cyclomatic Complexity**
  * [Cyclomatic Complexity Wikipedia](https://en.wikipedia.org/wiki/Cyclomatic_complexity) 
  * [Cyclomatic Complexity](https://www.ibm.com/docs/en/raa/6.1?topic=metrics-cyclomatic-complexity) by IBM.

## Example

```text
File: src/main/java/net/floriankraemer/cognitive_analysis/domain/ComplexityCalculator.java
┌─────────────────────────────────────────────────────────────────┬────────────────────┬─────┬─────────┬───────┬─────────┬────────┬──┬──────────┬────┬──────────┐
│Class                                                            │Method              │Lines│Arguments│Returns│Variables│Property│If│If Nesting│Else│Cognitive │
│                                                                 │                    │     │         │       │         │Accesses│  │  Level   │    │Complexity│
├─────────────────────────────────────────────────────────────────┼────────────────────┼─────┼─────────┼───────┼─────────┼────────┼──┼──────────┼────┼──────────┤
│net.floriankraemer.cognitive_analysis.domain.ComplexityCalculator│ComplexityCalculator│  3  │    1    │   0   │    1    │   1    │0 │    0     │ 0  │  0.000   │
├─────────────────────────────────────────────────────────────────┼────────────────────┼─────┼─────────┼───────┼─────────┼────────┼──┼──────────┼────┼──────────┤
│net.floriankraemer.cognitive_analysis.domain.ComplexityCalculator│calculate           │ 11  │    1    │   0   │    5    │   1    │0 │    0     │ 0  │  0.182   │
│                                                                 │                    │     │         │       │  0.182  │        │  │          │    │          │
├─────────────────────────────────────────────────────────────────┼────────────────────┼─────┼─────────┼───────┼─────────┼────────┼──┼──────────┼────┼──────────┤
│net.floriankraemer.cognitive_analysis.domain.ComplexityCalculator│weight              │  6  │    2    │   2   │    0    │   0    │1 │    1     │ 0  │  0.000   │
└─────────────────────────────────────────────────────────────────┴────────────────────┴─────┴─────────┴───────┴─────────┴────────┴──┴──────────┴────┴──────────┘

Cognitive metrics calculated for src/main/java/net/floriankraemer/cognitive_analysis/domain/ComplexityCalculator.java
```

## License

Copyright Florian Krämer

Licensed under the [GPL3 license](LICENSE).
