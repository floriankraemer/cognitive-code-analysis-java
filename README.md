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

Write an HTML, CSV or JSON report instead:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java --report-type html --report-file report.html
```

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
