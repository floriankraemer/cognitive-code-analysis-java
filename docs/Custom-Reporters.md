# Custom Reporters

A custom reporter writes a report format of your own.
You implement one small interface, build a jar, and register the class in the configuration.
`analyse` and `churn` then accept its key as `--report-type`.

## The interfaces

The interfaces and their input records are the public API.
They live in the package `net.floriankraemer.cognitive_analysis.report.spi`, and everything in it is immutable and independent of the internals of the analysis.

```java
public interface CognitiveReporter {
  String render(CognitiveReportInput input);
}

public interface ChurnReporter {
  String render(ChurnReportInput input);
}
```

`render` returns the complete report.
It is written to the report file as UTF-8.
A `RuntimeException` or a linkage error such as a `NoClassDefFoundError` fails the run with exit status 1, and the message is shown as `Reporter <key> failed: <message>`.
No file is written in that case.

`CognitiveReportInput` has:

- `methods`: the analysed methods, in the order of the run (see `--sort-by`).
  Each has `file`, `className`, `methodName`, `line`, `score`, `cyclomatic`, `understandability`, `halstead`, `coverage` and `metrics`.
  `halstead` has the counts `distinctOperators`, `distinctOperands`, `totalOperators` and `totalOperands`, and the values `volume`, `difficulty` and `effort`.
  `coverage` is the line coverage between 0.0 and 1.0, or `null` without a coverage report, see [Coverage](./Coverage.md).
  `metrics` maps the key of each of the eight scored metrics (`lineCount`, `argCount`, `returnCount`, `variableCount`, `propertyCallCount`, `ifCount`, `ifNestingLevel`, `elseCount`) to its `count`, its `weight` and its `weightDelta`.
  The `weightDelta` is `null` when there is no baseline.
- `settings`: `scoreThreshold`, `showOnlyMethodsExceedingThreshold`, `showHalsteadComplexity`, `showCyclomaticComplexity`, `showUnderstandability`, `groupByClass` and `showDetailedCognitiveMetrics`.
  The built-in reports use them to shape their output, and a custom reporter may ignore them.

`ChurnReportInput` has `classes`, the churn of every class in the order of the run.
Each entry has `className`, `file`, `score`, `timesChanged` and `churn`, and `coverage`, `riskChurn` and `riskLevel`, which are `null` without coverage data.

## Rules for the class

- It implements `CognitiveReporter` for `analyse`, or `ChurnReporter` for `churn`.
- It is a public, non-abstract class with a public constructor without parameters.
  A new instance is created for each run.
- `render` must not return `null`; that fails the run with `Reporter <key> returned no output (null).`
- It may use any library you put into the jar or the class path of the application.
  The jar is loaded by its own class loader, whose parent is the class loader of the application, so the SPI types are shared.

## Example

A reporter that prints one line per method above the score threshold:

```java
package com.example;

import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReportInput;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReporter;

public class HotspotReporter implements CognitiveReporter {

  @Override
  public String render(CognitiveReportInput input) {
    StringBuilder out = new StringBuilder();
    for (CognitiveReportInput.Method method : input.methods()) {
      if (method.score() > input.settings().scoreThreshold()) {
        out.append(String.format("%s::%s (%s:%d) score %.3f%n",
            method.className(), method.methodName(), method.file(), method.line(), method.score()));
      }
    }
    return out.toString();
  }
}
```

Build it as a jar that does not contain the SPI, because the application provides it.
Publish or install `cognitive-analysis` into your Maven repository first, for example with `./mvnw install`.

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>hotspot-reporter</artifactId>
  <version>1.0.0</version>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>net.floriankraemer</groupId>
      <artifactId>cognitive-analysis</artifactId>
      <version>0.0.1-SNAPSHOT</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>
</project>
```

`mvn package` creates `target/hotspot-reporter-1.0.0.jar`.

Register it in the configuration:

```yaml
cognitive:
  customReporters:
    cognitive:
      hotspots: {class: com.example.HotspotReporter, jar: libs/hotspot-reporter-1.0.0.jar}
    churn: {}
```

Run it:

```shell
java -jar target/cognitive-analysis.jar analyse src/main/java -c jcca.yaml -r hotspots -f hotspots.txt
```

A churn reporter works the same way under `customReporters.churn`, and is used with `churn -r <key>`.

## Configuration

Each key has a `class` and an optional `jar`.

- A relative `jar` is resolved against the directory of the configuration file, not against the working directory.
  An absolute path is used as it is.
  The path is literal: `~` and environment variables are not expanded.
- The jar must be compiled for Java 21 or newer, the version the application runs on.
- Without a `jar`, the class must be on the class path of the application.
- The key must not be the name of a built-in report type of the same command, in any letter case.
  The run is rejected with `Custom reporter key '<key>' conflicts with a built-in report type`.
- Built-in types are matched first and in any letter case.
  A custom key is matched exactly.
- The shape of the section is validated when the configuration is loaded.
  The collision with a built-in type is checked when you select a reporter of that command.
  Only the reporter that you select with `-r` is loaded, and it is loaded before the analysis starts, so a broken reporter fails fast.

## Errors

Each error is printed to standard error and the exit status is 1.

| Situation | Message |
| --- | --- |
| The jar does not exist | `Reporter jar not found: <path>` |
| The class is not in the jar or on the class path | `Reporter class not found: <class>` |
| The class has the wrong interface | `Reporter <class> must implement <interface>` |
| There is no public constructor without parameters | `Reporter <class> must have a public constructor without parameters` |
| The constructor or a static initializer fails | `Reporter <class> could not be created: <cause>` |
| `render` throws | `Reporter <key> failed: <cause>` |
| `render` returns `null` | `Reporter <key> returned no output (null).` |
| The type is neither built-in nor a custom key | `Invalid report type '<type>'. Expected one of: <built-in types>, <custom keys>.` |
