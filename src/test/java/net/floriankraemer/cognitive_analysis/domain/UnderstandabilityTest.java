package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Expected values follow the SonarSource white paper "Cognitive Complexity" by G. Ann Campbell. The
 * per-line increments are commented in the fixtures under src/test/resources/fixtures/understandability.
 */
class UnderstandabilityTest {

  private static final Path FIXTURES = Path.of("src/test/resources/fixtures/understandability");

  @Test
  void reproducesTheExamplesOfTheWhitePaper() throws Exception {
    assertThat(describe("WhitePaper.java")).isEqualTo("""
        sumOfPrimes total=7 structural=3 fundamental=1 nesting=3
        getWords total=1 structural=1
        overriddenSymbolFrom total=19 structural=7 hybrid=1 fundamental=1 nesting=10
        addVersion total=35 structural=11 fundamental=2 nesting=22
        toRegexp total=20 structural=5 hybrid=6 fundamental=3 nesting=6
        """);
  }

  @Test
  void appliesEachRuleOfTheSpecification() throws Exception {
    assertThat(describe("Rules.java")).isEqualTo("""
        Rules total=1 structural=1
        empty total=0
        ifOnly total=1 structural=1
        ifElse total=2 structural=1 hybrid=1
        elseIfChainIsNotNestedDeeper total=12 structural=5 hybrid=3 nesting=4
        elseBodyNests total=4 structural=2 hybrid=1 nesting=1
        ternary total=1 structural=1
        nestedTernary total=3 structural=2 nesting=1
        ternaryInConditionIsNotNested total=2 structural=2
        switchCountsOnceNotPerCase total=1 structural=1
        switchExpressionNestsItsArms total=3 structural=2 nesting=1
        loops total=4 structural=4
        nestedLoops total=6 structural=3 nesting=3
        doWhileBodyNestsButConditionDoesNot total=4 structural=2 fundamental=1 nesting=1
        tryAndFinallyDoNotCount total=4 structural=3 nesting=1
        lambdaNests total=2 structural=1 nesting=1
        lambdaInLambdaNestsTwice total=3 structural=1 nesting=2
        lambdaInsideIfNests total=4 structural=2 nesting=2
        anonymousAndLocalClassesAreSkipped total=0
        likeOperators total=1 fundamental=1
        likeOrOperators total=1 fundamental=1
        andThenOr total=2 fundamental=2
        orThenAnd total=2 fundamental=2
        mixedSequence total=3 fundamental=3
        parenthesesDoNotBreakASequence total=1 fundamental=1
        negationStartsANewSequence total=2 fundamental=2
        bitwiseOperatorsDoNotCount total=0
        sequenceInACallArgumentIsItsOwn total=2 fundamental=2
        labeledBreakAndContinue total=17 structural=6 fundamental=2 nesting=9
        recursionCountsOnce total=2 structural=1 fundamental=1 recursion=1
        callOnOtherObjectIsNoRecursion total=0
        recursionInLambda total=1 fundamental=1 recursion=1
        synchronizedDoesNotNest total=1 structural=1
        run total=0
        check total=0
        """);
  }

  @Test
  void countsCallsToTheOwnMethodWithoutScopeOrThroughTheOwnClassOnly() throws Exception {
    assertThat(describe("Recursion.java")).isEqualTo("""
        className total=1 fundamental=1 recursion=1
        qualifiedThis total=1 fundamental=1 recursion=1
        countsOnce total=1 fundamental=1 recursion=1
        overload(int) total=1 fundamental=1 recursion=1
        overload(String) total=0
        toString total=0
        otherClassName total=0
        otherObject total=0
        innerThis total=1 fundamental=1 recursion=1
        outerThis total=0
        outerClassName total=0
        """);
  }

  @Test
  void restoresTheBreakdownReadOnlyAndInOrder() {
    final Map<String, Integer> source = new LinkedHashMap<>();
    source.put("total", 1);
    source.put("structural", 1);

    final Understandability understandability = new Understandability(1, source);
    source.put("hybrid", 2);

    assertThat(understandability.breakdown()).containsExactly(Map.entry("total", 1), Map.entry("structural", 1));
    assertThatThrownBy(() -> understandability.breakdown().put("x", 1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void breakdownHasAllKeysInAStableOrder() {
    final CognitiveMetrics metrics = new CognitiveMetrics("A", "m", 1);

    assertThat(metrics.getUnderstandability().breakdown()).containsExactly(
        Map.entry("total", 0), Map.entry("structural", 0), Map.entry("hybrid", 0),
        Map.entry("fundamental", 0), Map.entry("nesting", 0), Map.entry("recursion", 0));
    assertThat(metrics.getUnderstandability().riskLevel()).isEqualTo("low");
  }

  @Test
  void totalIsTheSumOfStructuralHybridFundamentalAndNesting() {
    final Understandability understandability = Understandability.of(new Understandability.Increments(2, 3, 4, 5, 1));

    assertThat(understandability.complexity()).isEqualTo(14);
    assertThat(understandability.breakdown()).containsExactly(
        Map.entry("total", 14), Map.entry("structural", 2), Map.entry("hybrid", 3),
        Map.entry("fundamental", 4), Map.entry("nesting", 5), Map.entry("recursion", 1));
  }

  @ParameterizedTest
  @CsvSource({
    "0,low", "5,low", "6,medium", "10,medium", "11,high", "15,high", "16,very_high", "100,very_high"
  })
  void mapsComplexityToTheCyclomaticRiskLevels(final int complexity, final String risk) {
    assertThat(new Understandability(complexity, Map.of()).riskLevel()).isEqualTo(risk);
  }

  private static String describe(final String fixture) throws Exception {
    final Map<String, CognitiveMetrics> metrics =
        new CognitiveMetricsCollector().collect(Files.readString(FIXTURES.resolve(fixture)));
    return metrics.values().stream()
        .map(m -> m.getMethodName() + " " + describe(m.getUnderstandability()))
        .collect(Collectors.joining("\n", "", "\n"));
  }

  private static String describe(final Understandability understandability) {
    final StringBuilder text = new StringBuilder("total=" + understandability.complexity());
    understandability.breakdown().forEach((kind, count) -> {
      if (count > 0 && !kind.equals("total")) {
        text.append(' ').append(kind).append('=').append(count);
      }
    });
    return text.toString();
  }
}
