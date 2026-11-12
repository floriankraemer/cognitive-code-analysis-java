package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Expected values are worked out by hand from src/test/resources/fixtures/halstead/Operations.java. */
class HalsteadMetricsTest {

  private static final Path FIXTURE = Path.of("src/test/resources/fixtures/halstead/Operations.java");

  private final Map<String, CognitiveMetrics> metrics = collect();

  @Test
  void countsDistinctAndTotalOperatorsAndOperandsOfTheFixtureMethods() {
    final String actual = metrics.values().stream()
        .map(m -> m.getMethodName() + " " + describe(m.getHalstead()))
        .collect(Collectors.joining("\n", "", "\n"));

    assertThat(actual).isEqualTo("""
        empty n1=0 n2=0 N1=0 N2=0
        add n1=1 n2=2 N1=1 N2=2
        assignmentsAndUnary n1=3 n2=3 N1=3 N2=5
        chainedCalls n1=2 n2=1 N1=2 N2=1
        creation n1=1 n2=1 N1=1 N2=1
        ternaryAndInstanceOf n1=2 n2=3 N1=2 N2=3
        literals n1=1 n2=8 N1=1 N2=8
        foo n1=0 n2=0 N1=0 N2=0
        repeated n1=2 n2=2 N1=4 N2=5
        fieldAccess n1=1 n2=2 N1=1 N2=3
        anonymousAndLocalClassesAreSkipped n1=1 n2=1 N1=1 N2=1
        lambdasCount n1=3 n2=5 N1=3 N2=5
        unaryVersusBinary n1=3 n2=1 N1=3 N2=2
        methodReferences n1=5 n2=1 N1=5 N2=2
        methodReferenceOnAnExpression n1=3 n2=2 N1=3 N2=3
        qualifiedCreation n1=1 n2=1 N1=1 N2=1
        """);
    // add: operators {+}; operands a, b.
    // assignmentsAndUnary: operators +=, =, - ; operands a (the declarator "int x" is neither), x, 1, x, x.
    // chainedCalls: call:length, call:trim; operand s.
    // creation: new:StringBuilder; operand n.
    // ternaryAndInstanceOf: operators ?:, instanceof; operands o, 1, 2.
    // literals: call:foo; operands "s", 'c', 1, 2L, 1.5, true, null and the text block.
    // repeated: ((a + (a * a)) + 1) + 1 has operators + + * +, operands a a a 1 1.
    // fieldAccess: operator +; operands count (this.count), count and h (h.count).
    // anonymousAndLocalClassesAreSkipped: new:Object and the operand a; both class bodies are skipped.
    // unaryVersusBinary: -x + +x has the three operators +, u- and u+; operand x twice.
    // methodReferences: call:forEach, call:stream, call:map and the references call:println and
    // call:length; operand list twice. The parser reads the scopes System.out and String as types, so they
    // are no operands.
    // methodReferenceOnAnExpression: call:forEach, call:equals, call:get; operands list twice and 0.
    // qualifiedCreation: new:Holder; the scope outer is an operand.
    // lambdasCount: call:forEach, call:println, *; operands list, out, System, x, 2.
  }

  @Test
  void derivesVolumeDifficultyAndEffortUnrounded() {
    // add: length 3, vocabulary 3, volume 3 * log2(3), difficulty (1 / 2) * (2 / 2) = 0.5.
    final HalsteadMetrics add = metrics.get("fixtures.halstead.Operations::add").getHalstead();
    assertThat(add.length()).isEqualTo(3);
    assertThat(add.vocabulary()).isEqualTo(3);
    assertThat(add.volume()).isCloseTo(3 * Math.log(3) / Math.log(2), within(1e-12));
    assertThat(add.difficulty()).isEqualTo(0.5);
    assertThat(add.effort()).isCloseTo(0.5 * add.volume(), within(1e-12));

    // repeated: length 9, vocabulary 4, volume 9 * 2 = 18, difficulty (2 / 2) * (5 / 2) = 2.5, effort 45.
    final HalsteadMetrics repeated = metrics.get("fixtures.halstead.Operations::repeated").getHalstead();
    assertThat(repeated.length()).isEqualTo(9);
    assertThat(repeated.vocabulary()).isEqualTo(4);
    assertThat(repeated.volume()).isCloseTo(18.0, within(1e-12));
    assertThat(repeated.difficulty()).isEqualTo(2.5);
    assertThat(repeated.effort()).isCloseTo(45.0, within(1e-12));
  }

  @Test
  void volumeUsesAnExactLog2ForPowersOfTwo() {
    // length 9, vocabulary 8: 9 * 3 must be exactly 27.0.
    assertThat(new HalsteadMetrics(4, 4, 4, 5).volume()).isEqualTo(27.0);
    assertThat(new HalsteadMetrics(1, 1, 1, 1).volume()).isEqualTo(2.0);
  }

  @Test
  void anEmptyMethodHasAllZeros() {
    final HalsteadMetrics empty = metrics.get("fixtures.halstead.Operations::empty").getHalstead();

    assertThat(empty.length()).isZero();
    assertThat(empty.vocabulary()).isZero();
    assertThat(empty.volume()).isZero();
    assertThat(empty.difficulty()).isZero();
    assertThat(empty.effort()).isZero();
  }

  @Test
  void difficultyIsZeroWithoutOperandsAndVolumeIsZeroForAVocabularyOfOne() {
    final HalsteadMetrics onlyOperators = new HalsteadMetrics(2, 0, 3, 0);
    assertThat(onlyOperators.difficulty()).isZero();
    assertThat(onlyOperators.volume()).isCloseTo(3 * 1.0, within(1e-12));

    final HalsteadMetrics singleSymbol = new HalsteadMetrics(1, 0, 5, 0);
    assertThat(singleSymbol.volume()).isZero();
  }

  private static Map<String, CognitiveMetrics> collect() {
    try {
      return new CognitiveMetricsCollector().collect(Files.readString(FIXTURE));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String describe(final HalsteadMetrics halstead) {
    return "n1=" + halstead.distinctOperators() + " n2=" + halstead.distinctOperands()
        + " N1=" + halstead.totalOperators() + " N2=" + halstead.totalOperands();
  }
}
