package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.HalsteadMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReportInput;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReportInput.Halstead;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReportInput.Metric;
import org.junit.jupiter.api.Test;

class ReporterInputsTest {

  private static CognitiveMetrics metrics(final double argWeight) {
    final CognitiveMetrics metrics = new CognitiveMetrics("a.B", "run", 7);
    metrics.setValue(MetricName.ARG_COUNT, 3);
    metrics.setWeight(MetricName.ARG_COUNT, argWeight);
    metrics.setCognitiveComplexity(1.5);
    metrics.setHalstead(new HalsteadMetrics(2, 3, 4, 5));
    return metrics;
  }

  @Test
  void mapsAMethodAndTheSettings() {
    final List<ScoredMethod> methods = List.of(new ScoredMethod("src/B.java", "a.B::run", metrics(2.0)));

    final CognitiveReportInput input = ReporterInputs.of(methods, TestConfigs.defaults());

    final CognitiveReportInput.Method method = input.methods().get(0);
    assertThat(method.file()).isEqualTo("src/B.java");
    assertThat(method.className()).isEqualTo("a.B");
    assertThat(method.methodName()).isEqualTo("run");
    assertThat(method.line()).isEqualTo(7);
    assertThat(method.score()).isEqualTo(1.5);
    assertThat(method.cyclomatic()).isEqualTo(1);
    assertThat(method.understandability()).isZero();
    assertThat(method.metrics().keySet()).containsExactly(
        "lineCount", "argCount", "returnCount", "variableCount", "propertyCallCount", "ifCount", "ifNestingLevel",
        "elseCount");
    assertThat(method.metrics().get("argCount")).isEqualTo(new Metric(3, 2.0, null));
    final HalsteadMetrics halstead = new HalsteadMetrics(2, 3, 4, 5);
    assertThat(method.halstead())
        .isEqualTo(new Halstead(2, 3, 4, 5, halstead.volume(), halstead.difficulty(), halstead.effort()));
    assertThat(input.settings()).isEqualTo(
        new CognitiveReportInput.Settings(0.5, false, false, false, false, true, true));
  }

  @Test
  void carriesTheWeightDeltaAgainstTheBaseline() {
    final CognitiveMetrics current = metrics(2.0);
    current.calculateDeltas(metrics(0.5));

    final CognitiveReportInput input = ReporterInputs.of(
        List.of(new ScoredMethod("src/B.java", "a.B::run", current)), TestConfigs.defaults());

    assertThat(input.methods().get(0).metrics().get("argCount").weightDelta()).isEqualTo(1.5);
  }

  @Test
  void theInputIsImmutable() {
    final List<CognitiveReportInput.Method> methods = new ArrayList<>(
        ReporterInputs.of(List.of(new ScoredMethod("f", "a.B::run", metrics(1))), TestConfigs.defaults()).methods());
    final Map<String, Metric> values = new LinkedHashMap<>(methods.get(0).metrics());
    final CognitiveReportInput input = new CognitiveReportInput(methods, null);
    methods.clear();
    values.clear();

    assertThat(input.methods()).hasSize(1);
    assertThat(input.methods().get(0).metrics()).isNotEmpty();
    assertThatThrownBy(() -> input.methods().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> input.methods().get(0).metrics().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
