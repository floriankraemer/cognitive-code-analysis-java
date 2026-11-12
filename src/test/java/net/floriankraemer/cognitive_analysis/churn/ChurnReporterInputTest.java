package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import net.floriankraemer.cognitive_analysis.churn.ChurnMetrics.RiskLevel;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.report.spi.ChurnReportInput;
import net.floriankraemer.cognitive_analysis.report.spi.ChurnReporter;
import org.junit.jupiter.api.Test;

class ChurnReporterInputTest {

  static final List<ChurnReportInput> CAPTURED = new ArrayList<>();

  public static class Capture implements ChurnReporter {
    @Override
    public String render(final ChurnReportInput input) {
      CAPTURED.add(input);
      return "ok";
    }
  }

  @Test
  void handsTheChurnOfEveryClassToTheReporter() throws Exception {
    CAPTURED.clear();
    final var config = TestConfigs.load("cognitive:\n  customReporters:\n    churn:\n      capture: {class: "
        + Capture.class.getName() + "}\n");
    final List<ChurnMetrics> churn = List.of(
        new ChurnMetrics("a.B", "B.java", 2.5, 3, 7.5, 0.5, 3.75, RiskLevel.HIGH),
        new ChurnMetrics("a.C", "C.java", 1, 1, 1, null, null, null));

    final String content = new ChurnReportBuilder(Clock.systemUTC()).resolve("capture", config).render(churn);

    assertThat(content).isEqualTo("ok");
    assertThat(CAPTURED).hasSize(1);
    assertThat(CAPTURED.get(0).classes()).containsExactly(
        new ChurnReportInput.Entry("a.B", "B.java", 2.5, 3, 7.5, 0.5, 3.75, "HIGH"),
        new ChurnReportInput.Entry("a.C", "C.java", 1, 1, 1, null, null, null));
  }
}
