package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.Help.Ansi;

class MetricFormatterTest {

  private static final String RED = "\u001B[31m";
  private static final String YELLOW = "\u001B[33m";
  private static final String GREEN = "\u001B[32m";

  private final MetricFormatter colors = new MetricFormatter(0.5, Ansi.ON);
  private final MetricFormatter plain = new MetricFormatter(0.5, Ansi.OFF);

  @Test
  void showsTheWeightWithThreeDecimalsRegardlessOfTheLocale() {
    Locale original = Locale.getDefault();
    Locale.setDefault(Locale.GERMANY);
    try {
      assertThat(plain.weighted(4, 1.0986)).isEqualTo("4 (1.099)");
      assertThat(plain.weighted(0, 0)).isEqualTo("0 (0.000)");
    } finally {
      Locale.setDefault(original);
    }
  }

  @Test
  void colorsTheScoreRedOnlyAboveTheThreshold() {
    assertThat(colors.score(0.501)).startsWith(RED);
    assertThat(colors.score(0.5)).startsWith(GREEN);
    assertThat(colors.score(0)).startsWith(GREEN);
  }

  @Test
  void colorsTheHalsteadVolumeFromOneHundredAndOneThousand() {
    assertThat(colors.halsteadVolume(99.99)).doesNotContain("\u001B");
    assertThat(colors.halsteadVolume(100)).startsWith(YELLOW);
    assertThat(colors.halsteadVolume(999.99)).startsWith(YELLOW);
    assertThat(colors.halsteadVolume(1000)).startsWith(RED);
  }

  @Test
  void colorsTheHalsteadDifficultyFromTenAndFifty() {
    assertThat(colors.halsteadDifficulty(9.99)).doesNotContain("\u001B");
    assertThat(colors.halsteadDifficulty(10)).startsWith(YELLOW);
    assertThat(colors.halsteadDifficulty(49.99)).startsWith(YELLOW);
    assertThat(colors.halsteadDifficulty(50)).startsWith(RED);
  }

  @Test
  void colorsTheHalsteadEffortFromFiveHundredAndFiveThousand() {
    assertThat(colors.halsteadEffort(499.99)).doesNotContain("\u001B");
    assertThat(colors.halsteadEffort(500)).startsWith(YELLOW);
    assertThat(colors.halsteadEffort(4999.99)).startsWith(YELLOW);
    assertThat(colors.halsteadEffort(5000)).startsWith(RED);
  }

  @Test
  void showsTheHalsteadValuesWithTwoDecimals() {
    assertThat(plain.halsteadVolume(7.5)).isEqualTo("7.50");
    assertThat(plain.halsteadDifficulty(1)).isEqualTo("1.00");
    assertThat(plain.halsteadEffort(0)).isEqualTo("0.00");
  }

  @Test
  void colorsOnlyTheRiskLevelAndLeavesLowUncolored() {
    assertThat(colors.withRisk(3, "low")).isEqualTo("3 (low)");
    assertThat(colors.withRisk(7, "medium")).startsWith("7 (" + YELLOW + "medium");
    assertThat(colors.withRisk(12, "high")).startsWith("12 (" + RED + "high");
    assertThat(colors.withRisk(20, "very_high")).startsWith("20 (" + RED + "very_high");
  }

  @Test
  void dropsTheColorsWhenAnsiIsOff() {
    assertThat(plain.score(9)).isEqualTo("9.000");
    assertThat(plain.withRisk(20, "very_high")).isEqualTo("20 (very_high)");
    assertThat(plain.halsteadVolume(5000)).isEqualTo("5000.00");
  }
}
