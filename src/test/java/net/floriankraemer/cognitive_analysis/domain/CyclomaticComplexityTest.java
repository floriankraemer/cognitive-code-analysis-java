package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Expected values are worked out by hand from src/test/resources/fixtures/cyclomatic/Decisions.java. */
class CyclomaticComplexityTest {

  private static final Path FIXTURE = Path.of("src/test/resources/fixtures/cyclomatic/Decisions.java");

  @Test
  void countsEachDecisionPointOfTheFixtureMethods() throws Exception {
    final Map<String, CognitiveMetrics> metrics = new CognitiveMetricsCollector().collect(Files.readString(FIXTURE));

    final String actual = metrics.values().stream()
        .map(m -> m.getMethodName() + " " + describe(m.getCyclomatic()))
        .collect(Collectors.joining("\n", "", "\n"));

    // base 1 plus: a lone if=1; else adds nothing; else-if is an if; each case entry with labels is
    // one however many labels it has; default adds nothing; multi-catch is one clause; && and ||
    // count one each; nested ternaries count each; lambdas count, anonymous and local classes do not.
    assertThat(actual).isEqualTo("""
        empty total=1
        ifOnly total=2 if=1
        elseDoesNotCount total=2 if=1
        elseIfChain total=3 if=2
        switchWithDefault total=3 case=2
        switchMultiLabelArrow total=3 case=2
        switchExpression total=3 case=2
        loops total=5 while=1 doWhile=1 for=1 forEach=1
        catches total=3 catch=2
        logical total=3 logicalAnd=1 logicalOr=1
        ternary total=2 ternary=1
        nestedTernary total=3 ternary=2
        lambdasCount total=4 if=2 logicalAnd=1
        anonymousAndLocalClassesAreSkipped total=2 if=1
        creationArgumentsCount total=2 ternary=1
        everything total=11 if=1 case=1 while=1 doWhile=1 for=1 forEach=1 catch=1 logicalAnd=1 logicalOr=1 ternary=1
        guardedPattern total=3 case=1 logicalAnd=1
        nullAndDefault total=1
        switchInLambda total=3 case=2
        doWhileBodyCounts total=3 if=1 doWhile=1
        """);
  }

  @Test
  void breakdownHasAllKeysInAStableOrder() {
    final CognitiveMetrics metrics = new CognitiveMetrics("A", "m", 1);

    assertThat(metrics.getCyclomatic().breakdown()).containsExactly(
        Map.entry("total", 1), Map.entry("base", 1), Map.entry("if", 0), Map.entry("case", 0),
        Map.entry("while", 0), Map.entry("doWhile", 0), Map.entry("for", 0), Map.entry("forEach", 0),
        Map.entry("catch", 0), Map.entry("logicalAnd", 0), Map.entry("logicalOr", 0), Map.entry("ternary", 0));
  }

  @ParameterizedTest
  @CsvSource({
    "1,low", "5,low", "6,medium", "10,medium", "11,high", "15,high", "16,very_high", "100,very_high"
  })
  void mapsComplexityToRiskLevelAtTheBoundaries(final int complexity, final String risk) {
    assertThat(new CyclomaticComplexity(complexity, Map.of()).riskLevel()).isEqualTo(risk);
  }

  private static String describe(final CyclomaticComplexity cyclomatic) {
    final StringBuilder text = new StringBuilder("total=" + cyclomatic.complexity());
    cyclomatic.breakdown().forEach((kind, count) -> {
      if (count > 0 && !kind.equals("total") && !kind.equals("base")) {
        text.append(' ').append(kind).append('=').append(count);
      }
    });
    return text.toString();
  }
}
