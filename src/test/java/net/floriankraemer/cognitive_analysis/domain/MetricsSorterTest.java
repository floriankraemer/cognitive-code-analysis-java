package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MetricsSorterTest {

  /** Three methods whose rank differs in every field: a is lowest, c is highest, in the order b, a, c. */
  private final ScoredMethod a = method("z/A.java", "p.Alpha", "one", m -> {
    m.setCognitiveComplexity(1);
    m.setHalstead(new HalsteadMetrics(1, 1, 1, 1));
    m.setCyclomatic(new CyclomaticComplexity(1, Map.of()));
    m.setUnderstandability(new Understandability(1, Map.of()));
    for (MetricName name : MetricName.values()) {
      m.setValue(name, 1);
      m.setWeight(name, 0.1);
    }
  });
  private final ScoredMethod b = method("y/B.java", "p.beta", "Two", m -> {
    m.setCognitiveComplexity(2);
    m.setHalstead(new HalsteadMetrics(2, 2, 2, 2));
    m.setCyclomatic(new CyclomaticComplexity(2, Map.of()));
    m.setUnderstandability(new Understandability(2, Map.of()));
    for (MetricName name : MetricName.values()) {
      m.setValue(name, 2);
      m.setWeight(name, 0.2);
    }
  });
  private final ScoredMethod c = method("x/C.java", "p.Gamma", "three", m -> {
    m.setCognitiveComplexity(10);
    m.setHalstead(new HalsteadMetrics(3, 3, 3, 3));
    m.setCyclomatic(new CyclomaticComplexity(11, Map.of()));
    m.setUnderstandability(new Understandability(12, Map.of()));
    for (MetricName name : MetricName.values()) {
      m.setValue(name, 10);
      m.setWeight(name, 1.5);
    }
  });

  static List<String> numericFields() {
    return MetricsSorter.FIELDS.stream().filter(field -> !List.of("class", "method", "file").contains(field)).toList();
  }

  @ParameterizedTest
  @MethodSource("numericFields")
  void sortsNumbersByValueAscendingAndDescending(final String field) throws Exception {
    List<ScoredMethod> shuffled = List.of(c, a, b);

    assertThat(new MetricsSorter(field, "asc").sort(shuffled)).containsExactly(a, b, c);
    assertThat(new MetricsSorter(field, "desc").sort(shuffled)).containsExactly(c, b, a);
  }

  @Test
  void comparesNumbersNumericallyAndNotAsText() throws Exception {
    // as text "10" sorts before "2"
    assertThat(new MetricsSorter("score", "asc").sort(List.of(c, b, a))).containsExactly(a, b, c);
  }

  @Test
  void sortsClassNamesIgnoringTheLetterCase() throws Exception {
    // p.Alpha, p.beta, p.Gamma; a case-sensitive order would put p.beta last
    assertThat(new MetricsSorter("class", "asc").sort(List.of(c, b, a))).containsExactly(a, b, c);
    assertThat(new MetricsSorter("class", "desc").sort(List.of(a, b, c))).containsExactly(c, b, a);
  }

  @Test
  void sortsMethodNamesIgnoringTheLetterCase() throws Exception {
    // one, Two, three: a case-sensitive order would put "Two" first
    assertThat(new MetricsSorter("method", "asc").sort(List.of(b, c, a))).containsExactly(a, c, b);
  }

  @Test
  void sortsFilesByPath() throws Exception {
    // x/C.java, y/B.java, z/A.java
    assertThat(new MetricsSorter("file", "asc").sort(List.of(a, b, c))).containsExactly(c, b, a);
  }

  @Test
  void breaksTiesByClassAndThenMethodInBothDirections() throws Exception {
    ScoredMethod sameScoreB = method("f", "p.B", "m", m -> m.setCognitiveComplexity(5));
    ScoredMethod sameScoreA2 = method("f", "p.A", "z", m -> m.setCognitiveComplexity(5));
    ScoredMethod sameScoreA1 = method("f", "p.A", "y", m -> m.setCognitiveComplexity(5));
    List<ScoredMethod> input = List.of(sameScoreB, sameScoreA2, sameScoreA1);

    assertThat(new MetricsSorter("score", "asc").sort(input)).containsExactly(sameScoreA1, sameScoreA2, sameScoreB);
    assertThat(new MetricsSorter("score", "desc").sort(input)).containsExactly(sameScoreA1, sameScoreA2, sameScoreB);
  }

  @Test
  void keepsTheGivenOrderOfMethodsThatAreEqualInEveryRespect() throws Exception {
    ScoredMethod first = method("f1", "p.A", "m", m -> { });
    ScoredMethod second = method("f2", "p.A", "m", m -> { });

    assertThat(new MetricsSorter("score", "asc").sort(List.of(second, first))).containsExactly(second, first);
  }

  @Test
  void acceptsTheOrderInAnyLetterCase() throws Exception {
    assertThat(new MetricsSorter("score", "DESC").sort(List.of(a, c, b))).containsExactly(c, b, a);
    assertThat(new MetricsSorter("score", "Asc").sort(List.of(c, b, a))).containsExactly(a, b, c);
  }

  @Test
  void leavesTheInputUntouchedAndReturnsANewList() throws Exception {
    List<ScoredMethod> input = List.of(c, a, b);

    List<ScoredMethod> sorted = new MetricsSorter("score", "asc").sort(input);

    assertThat(sorted).isNotSameAs(input);
    assertThat(input).containsExactly(c, a, b);
  }

  @Test
  void keepsTheGivenOrderWithoutAField() throws Exception {
    List<ScoredMethod> input = List.of(c, a, b);

    assertThat(new MetricsSorter(null, "desc").sort(input)).containsExactly(c, a, b);
  }

  @Test
  void listsTheFieldsInTheDocumentedOrder() {
    assertThat(MetricsSorter.FIELDS).containsExactly(
        "score", "halstead", "cyclomatic", "understandability", "class", "method", "file",
        "lineCount", "argCount", "returnCount", "variableCount", "propertyCallCount", "ifCount",
        "ifNestingLevel", "elseCount",
        "lineCountWeight", "argCountWeight", "returnCountWeight", "variableCountWeight",
        "propertyCallCountWeight", "ifCountWeight", "ifNestingLevelWeight", "elseCountWeight");
  }

  @Test
  void rejectsAnUnknownField() {
    assertThatThrownBy(() -> new MetricsSorter("bogus", "asc"))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Invalid sort field \"bogus\". Available fields: " + String.join(", ", MetricsSorter.FIELDS));
  }

  @Test
  void rejectsAFieldInTheWrongLetterCase() {
    assertThatThrownBy(() -> new MetricsSorter("Score", "asc"))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid sort field \"Score\"");
  }

  @Test
  void rejectsAnUnknownOrderNamingIt() {
    assertThatThrownBy(() -> new MetricsSorter("score", "up"))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Sort order must be \"asc\" or \"desc\", got \"up\"");
  }

  @Test
  void rejectsAnUnknownOrderWithoutAField() {
    assertThatThrownBy(() -> new MetricsSorter(null, ""))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Sort order must be \"asc\" or \"desc\", got \"\"");
  }

  @Test
  void reportsTheUnknownFieldBeforeTheUnknownOrder() {
    assertThatThrownBy(() -> new MetricsSorter("bogus", "up"))
        .hasMessageStartingWith("Invalid sort field");
  }

  private static ScoredMethod method(
      final String file, final String className, final String methodName, final Consumer<CognitiveMetrics> setup) {
    CognitiveMetrics metrics = new CognitiveMetrics(className, methodName, 1);
    setup.accept(metrics);
    return new ScoredMethod(file, className + "::" + methodName, metrics);
  }
}
