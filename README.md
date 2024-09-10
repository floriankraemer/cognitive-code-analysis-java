# Cognitive Complexity Analysis

Cognitive Code Analysis is an approach to understanding and improving code by focusing on how human cognition interacts with code. It emphasizes making code more readable, understandable, and maintainable by considering the cognitive processes of the developers who write and work with the code.

> "Human short-term or working memory was estimated to be limited to 7 ± 2 variables in the 1950s. A more current estimate is 4 ± 1 constructs. Decision quality generally becomes degraded once this limit of four constructs is exceeded."

[Source: Human Cognitive Limitations. Broad, Consistent, Clinical Application of Physiological Principles Will Require Decision Support](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5822395/)

## Running it

Cognitive Complexity Analysis

```bash
java -jar cognitive_analysis-0.0.1-SNAPSHOT.jar calculate-cognitive-metrics ".\\src\\main\\java\\"
```

## Documentation

* [Cognitive Complexity Analysis](./docs/Cognitive-Complexity-Analysis.md#cognitive-complexity-analysis)
    * [Why bother?](./docs/Cognitive-Complexity-Analysis.md#why-bother)
    * [What is the difference to Cyclomatic Complexity?](./docs/Cognitive-Complexity-Analysis.md#what-is-the-difference-to-cyclomatic-complexity)
    * [How is Cognitive Complexity calculated?](./docs/Cognitive-Complexity-Analysis.md#how-is-cognitive-complexity-calculated)
    * [Metrics Collected](./docs/Cognitive-Complexity-Analysis.md#metrics-collected)
    * [Result Interpretation](./docs/Cognitive-Complexity-Analysis.md#result-interpretation)
    * [Configuration](./docs/Configuration.md#configuration)
        * [Tuning the calculation](./docs/Configuration.md#tuning-the-calculation)
    * [Examples](#examples)
        * [Wordpress WP_Debug_Data](#wordpress-wp_debug_data)
        * [Doctrine Paginator](#doctrine-paginator)
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
File: C:\Users\florian\Desktop\Java\cognitive-code-analysis\.\src\main\java\net\floriankraemer\cognitive_analysis\domain\ScoreCalculator.java

┌───────────────┬──────────────────┬─────┬─────┬──────────┬─────┬─────┬──────┬──────┬──────┬────────┬─────────┬──────────┐
│Class          │Method            │Line │ If  │If-Nesting│Else │Loop │Switch│Method│Return│Argument│Try-Catch│Complexity│
│               │                  │Count│Count│  Level   │Count│Count│Count │ Call │Count │ Count  │ Nesting │          │
│               │                  │     │     │          │     │     │      │Count │      │        │  Level  │          │
├───────────────┼──────────────────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
│ScoreCalculator│calculateScore    │ 55  │  0  │    0     │  0  │  0  │  0   │  50  │  0   │   1    │    0    │0,875     │
├───────────────┼──────────────────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
│ScoreCalculator│getThreshold      │  3  │  0  │    0     │  0  │  0  │  0   │  0   │  1   │   0    │    0    │0,000     │
├───────────────┼──────────────────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
│ScoreCalculator│getScale          │  3  │  0  │    0     │  0  │  0  │  0   │  0   │  1   │   0    │    0    │0,000     │
├───────────────┼──────────────────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
│MetricConfig   │getThreshold      │  3  │  0  │    0     │  0  │  0  │  0   │  0   │  1   │   0    │    0    │0,000     │
├───────────────┼──────────────────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
│MetricConfig   │getScale          │  3  │  0  │    0     │  0  │  0  │  0   │  0   │  1   │   0    │    0    │0,000     │
├───────────────┼──────────────────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
│ScoreCalculator│calculate         │ 25  │  1  │    1     │  0  │  0  │  0   │  10  │  0   │   2    │    0    │0,182     │
├───────────────┼──────────────────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
│ScoreCalculator│calculateLogWeight│  6  │  1  │    1     │  0  │  0  │  0   │  1   │  3   │   3    │    0    │0,182     │
└───────────────┴──────────────────┴─────┴─────┴──────────┴─────┴─────┴──────┴──────┴──────┴────────┴─────────┴──────────┘
```

## License

Copyright Florian Krämer

Licensed under the [GPL3 license](LICENSE).
