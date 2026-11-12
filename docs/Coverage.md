# Coverage

The `analyse` and `churn` commands can read the line coverage of a test run.
Coverage adds a column to the console and the reports, and `churn` uses it to compute the risk of a class.
The tool reads a report that another tool has written, so it runs no tests itself.

## Supported formats

| Option | Format | Typical producer |
| --- | --- | --- |
| `--coverage-jacoco <file>` | JaCoCo XML report | JaCoCo Maven or Gradle plugin |
| `--coverage-cobertura <file>` | Cobertura XML report | Cobertura Maven plugin |
| `--coverage-clover <file>` | Clover XML report | OpenClover or Clover Maven plugin |

Only one of the options can be given in a run.
Giving two is an error: `Only one coverage format can be specified at a time.`

A missing file is reported as `Coverage file not found: <file>`.
A file that is not well-formed XML, or that is a report of another format, is reported as `Invalid coverage file <file>` with the reason.
The XML parser does not load external entities or external DTDs, so a report cannot make the tool read local files or the network.

## Producing a report

Run the tests with coverage first, then pass the report to the tool.
The output locations below are the defaults of the plugins for a Maven project.

```bash
./mvnw -B verify
java -jar target/cognitive-analysis.jar analyse src/main/java --coverage-jacoco target/site/jacoco/jacoco.xml
```

For a JaCoCo report of this repository, `./mvnw -B verify` writes `target/site/jacoco/jacoco.xml`.

## What is matched

Coverage is matched to the analysed methods by class and method, and the tool uses the line coverage of each.

* The class is matched by its fully qualified name, so `com.example.Outer.Inner` in the analysis matches the class `com/example/Outer$Inner` of JaCoCo or Cobertura.
* A method is matched to the method of the report with the same name whose first line lies inside the lines of the analysed method.
  A method of the report without line numbers is matched by its name, but only when no other method has that name.
* A method without a match takes the line coverage of its class.
* A method or class without any lines has no coverage and shows `N/A`.

Constructors are matched to the report entry `<init>`.
Overloads are matched by their name, and the first line tells them apart.

### Kotlin

The class names of Kotlin are the JVM names, so reports of Kotlin code match without configuration.
A top-level function of `Ledger.kt` is in the class `pkg.LedgerKt`, and a companion object is `pkg.Outer.Companion`.
An `internal fun audit()` is compiled to a method named `audit$<module>`, and it matches `audit`.

## Where coverage appears

* In `analyse`, the console shows a `Line Coverage` column, and the reports show a `Coverage` column.
  The column is shown only when a coverage option is given, and the values have two decimals with a percent sign, such as `83.33%`.
* In `churn`, the console and the reports show the columns `Risk Churn`, `Coverage` and `Risk Level`, see [Churn](./Churn.md#risk).

## Warnings

When the report knows none or only some of the analysed classes, the tool prints a warning before the results:

```text
Coverage report <file> matched none of the 3 analysed classes.
Coverage report <file> matched 2 of 3 analysed classes.
```

A warning of this kind almost always means that the report was produced from other sources or another module.
The analysis itself still runs, and classes without a match have no coverage.

## Notes

* Coverage is applied after the analysis, so it never changes the cache or the cognitive metrics.
* The coverage column is not part of the score.
  It only informs the reader, and `churn` uses it for the risk level.
