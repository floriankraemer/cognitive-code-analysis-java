package net.floriankraemer.cognitive_analysis.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsCollector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The three formats describe the same fixture class, {@code fixtures/coverage/Shapes.java}. */
class CoverageReadersTest {

  private static final Path FIXTURES = Path.of("src/test/resources/fixtures/coverage");
  private static final double PRECISION = 0.0001;

  private static Map<String, CognitiveMetrics> shapes;

  @TempDir
  Path tempDir;

  @BeforeAll
  static void parseTheFixtureSource() throws Exception {
    shapes = new CognitiveMetricsCollector().collect(Files.readString(FIXTURES.resolve("Shapes.java")));
  }

  private static CoverageData read(final CoverageFormat format, final String file) throws Exception {
    return format.read(FIXTURES.resolve(file));
  }

  private static double method(final CoverageData data, final String key) {
    return data.methodCoverage(shapes.get(key)).orElseThrow();
  }

  @Test
  void theFixtureSourceHasTheMethodsTheReportsDescribe() {
    assertThat(shapes.keySet()).containsExactly(
        "shapes.Shapes::Shapes", "shapes.Shapes::area(int)", "shapes.Shapes::area(int,int)",
        "shapes.Shapes::describe", "shapes.Shapes::unused",
        "shapes.Shapes.Inner::perimeter", "shapes.Shapes.Inner::never");
  }

  @Test
  void jacocoGivesTheLineCoverageOfEveryMethodAndMapsNestedAndOverloadedMethods() throws Exception {
    final CoverageData data = read(CoverageFormat.JACOCO, "jacoco.xml");

    assertThat(method(data, "shapes.Shapes::Shapes")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes::area(int)")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes::area(int,int)")).isEqualTo(0.8);
    assertThat(method(data, "shapes.Shapes::describe")).isEqualTo(0.8);
    assertThat(method(data, "shapes.Shapes::unused")).isEqualTo(0.0);
    assertThat(method(data, "shapes.Shapes.Inner::perimeter")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes.Inner::never")).isEqualTo(0.0);
  }

  @Test
  void jacocoMapsTheSlashAndDollarNamesOfClasses() throws Exception {
    final CoverageData data = read(CoverageFormat.JACOCO, "jacoco.xml");

    assertThat(data.classCoverage("shapes.Shapes")).hasValue(0.75);
    assertThat(data.classCoverage("shapes.Shapes.Inner")).hasValue(2.0 / 3);
    assertThat(data.classCoverage("shapes/Shapes$Inner")).isEmpty();
  }

  @Test
  void coberturaGivesTheLineRateOfEveryMethod() throws Exception {
    final CoverageData data = read(CoverageFormat.COBERTURA, "cobertura.xml");

    assertThat(method(data, "shapes.Shapes::Shapes")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes::area(int)")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes::area(int,int)")).isEqualTo(0.8);
    assertThat(method(data, "shapes.Shapes::unused")).isEqualTo(0.0);
    assertThat(method(data, "shapes.Shapes.Inner::perimeter")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes.Inner::never")).isEqualTo(0.0);
    assertThat(data.classCoverage("shapes.Shapes.Inner").orElseThrow()).isCloseTo(2.0 / 3, within(PRECISION));
  }

  @Test
  void cloverComputesTheMethodCoverageFromTheStatementsAfterTheMethodLine() throws Exception {
    final CoverageData data = read(CoverageFormat.CLOVER, "clover.xml");

    assertThat(method(data, "shapes.Shapes::Shapes")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes::area(int)")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes::area(int,int)")).isEqualTo(0.75);
    assertThat(method(data, "shapes.Shapes::describe")).isEqualTo(0.8);
    assertThat(method(data, "shapes.Shapes::unused")).isEqualTo(0.0);
    assertThat(method(data, "shapes.Shapes.Inner::perimeter")).isEqualTo(1.0);
    assertThat(method(data, "shapes.Shapes.Inner::never")).isEqualTo(0.0);
    assertThat(data.classCoverage("shapes.Shapes")).hasValue(0.75);
  }

  @Test
  void aMethodTheReportDoesNotKnowFallsBackToItsClass() throws Exception {
    final CognitiveMetrics ghost = unit("shapes.Shapes", "ghost", 100);

    for (final String[] report : new String[][] {
        {"JACOCO", "jacoco.xml"}, {"COBERTURA", "cobertura.xml"}, {"CLOVER", "clover.xml"}}) {
      final CoverageData data = read(CoverageFormat.valueOf(report[0]), report[1]);
      assertThat(data.methodCoverage(ghost)).as(report[0]).hasValue(0.75);
    }
  }

  @Test
  void aClassTheReportDoesNotKnowHasNoCoverage() throws Exception {
    final CognitiveMetrics untested = unit("shapes.Untested", "answer", 5);

    for (final CoverageFormat format : CoverageFormat.values()) {
      final CoverageData data = format.read(FIXTURES.resolve(fileOf(format)));
      assertThat(data.methodCoverage(untested)).as(format.name()).isEmpty();
      assertThat(data.classCoverage("shapes.Untested")).as(format.name()).isEmpty();
    }
  }

  private static String fileOf(final CoverageFormat format) {
    return format.name().toLowerCase(java.util.Locale.ROOT) + ".xml";
  }

  @Test
  void aSameNamedMethodOutsideTheLinesOfOursIsNotTaken() throws Exception {
    final CoverageData data = read(CoverageFormat.JACOCO, "jacoco.xml");
    // area(int) of the report starts at line 12, this one covers lines 40 to 41 only.
    final CognitiveMetrics elsewhere = new CognitiveMetrics("shapes.Shapes", "area(int)", 40);
    elsewhere.setValue(MetricName.LINE_COUNT, 2);

    assertThat(data.methodCoverage(elsewhere)).hasValue(0.75);
  }

  @Test
  void aMethodOfTheReportMustStartBeforeTheLineAfterOurLastLine() throws Exception {
    final CoverageData data = read(CoverageFormat.JACOCO, "jacoco.xml");
    final CognitiveMetrics atTheEnd = new CognitiveMetrics("shapes.Shapes", "unused", 33);
    atTheEnd.setValue(MetricName.LINE_COUNT, 3);
    final CognitiveMetrics justAfter = new CognitiveMetrics("shapes.Shapes", "unused", 33);
    justAfter.setValue(MetricName.LINE_COUNT, 2);

    // The report says line 36: lines 33 to 35 do not contain it, lines 33 to 36 and a method starting there do.
    assertThat(data.methodCoverage(atTheEnd)).hasValue(0.75);
    assertThat(data.methodCoverage(justAfter)).hasValue(0.75);
    final CognitiveMetrics inside = new CognitiveMetrics("shapes.Shapes", "unused", 33);
    inside.setValue(MetricName.LINE_COUNT, 4);
    assertThat(data.methodCoverage(inside)).hasValue(0.0);
    final CognitiveMetrics startsAtTheLine = new CognitiveMetrics("shapes.Shapes", "unused", 36);
    startsAtTheLine.setValue(MetricName.LINE_COUNT, 1);
    assertThat(data.methodCoverage(startsAtTheLine)).hasValue(0.0);
  }

  @Test
  void aMethodWithoutLinesInTheReportIsMatchedByNameWhenItIsTheOnlyOne() throws Exception {
    final Path report = write("lineless.xml", """
        <coverage><packages><package name="p"><classes>
          <class name="p.A" line-rate="0.5"><methods>
            <method name="one" line-rate="0.25"/>
            <method name="twice" line-rate="1.0"/>
            <method name="twice" line-rate="0.0"/>
          </methods></class>
        </classes></package></packages></coverage>""");
    final CoverageData data = CoverageFormat.COBERTURA.read(report);

    assertThat(data.methodCoverage(unit("p.A", "one", 3))).hasValue(0.25);
    assertThat(data.methodCoverage(unit("p.A", "twice", 3))).hasValue(0.5);
  }

  @Test
  void aMethodOrClassWithoutLinesHasNoCoverageOfItsOwn() throws Exception {
    final Path report = write("empty.xml", """
        <report name="x"><package name="p">
          <class name="p/A"><method name="m" desc="()V" line="3"><counter type="LINE" missed="0" covered="0"/></method></class>
        </package></report>""");
    final CoverageData data = CoverageFormat.JACOCO.read(report);

    assertThat(data.classCoverage("p.A")).isEmpty();
    assertThat(data.methodCoverage(unit("p.A", "m", 3))).isEmpty();
  }

  @Test
  void aCloverMethodWithoutStatementsIsCoveredWhenItWasCalled() throws Exception {
    final Path report = write("empty-methods.xml", """
        <coverage><project><package name="p"><file name="A.java">
          <class name="A"><metrics statements="0" coveredstatements="0"/></class>
          <line num="3" type="method" signature="called()" count="2"/>
          <line num="5" type="method" signature="idle()" count="0"/>
        </file></package></project></coverage>""");
    final CoverageData data = CoverageFormat.CLOVER.read(report);

    assertThat(data.methodCoverage(unit("p.A", "called", 3))).hasValue(1.0);
    assertThat(data.methodCoverage(unit("p.A", "idle", 5))).hasValue(0.0);
    assertThat(data.classCoverage("p.A")).isEmpty();
  }

  @Test
  void cloverKeepsAlreadyQualifiedClassNamesAndUsesTheNameAttributeOfMethods() throws Exception {
    final Path report = write("qualified.xml", """
        <coverage><project><package name="p"><file name="A.java">
          <class name="p.A"><metrics statements="4" coveredstatements="1"/></class>
          <line num="3" type="method" name="run" count="1"/>
          <line num="4" type="stmt" count="1"/>
        </file></package>
        <package name="default-pkg"><file name="B.java">
          <class name="B"><metrics statements="2" coveredstatements="2"/></class>
        </file></package></project></coverage>""");
    final CoverageData data = CoverageFormat.CLOVER.read(report);

    assertThat(data.classCoverage("p.A")).hasValue(0.25);
    assertThat(data.classCoverage("B")).hasValue(1.0);
    assertThat(data.methodCoverage(unit("p.A", "run", 3))).hasValue(1.0);
  }

  @Test
  void aCloverClassWithoutMetricsHasNoCoverage() throws Exception {
    final Path report = write("no-metrics.xml", """
        <coverage><project><file name="A.java"><class name="A"/></file></project></coverage>""");

    assertThat(CoverageFormat.CLOVER.read(report).classCoverage("A")).isEmpty();
  }

  @Test
  void rateAttributesThatAreNotNumbersAreIgnored() throws Exception {
    final Path report = write("garbled.xml", """
        <coverage><packages><package name="p"><classes>
          <class name="p.A" line-rate="many"/>
        </classes></package></packages></coverage>""");

    assertThat(CoverageFormat.COBERTURA.read(report).classCoverage("p.A")).isEmpty();
  }

  @Test
  void aFileOfAnotherFormatIsRejected() {
    assertThatThrownBy(() -> read(CoverageFormat.JACOCO, "cobertura.xml"))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageContaining("Invalid coverage file " + FIXTURES.resolve("cobertura.xml") + ": not a JaCoCo report");
    assertThatThrownBy(() -> read(CoverageFormat.COBERTURA, "clover.xml"))
        .hasMessageContaining("not a Cobertura report");
    assertThatThrownBy(() -> read(CoverageFormat.CLOVER, "cobertura.xml"))
        .hasMessageContaining("not a Clover report");
  }

  @Test
  void aMissingFileIsReported() {
    final Path missing = tempDir.resolve("missing.xml");

    assertThatThrownBy(() -> CoverageFormat.JACOCO.read(missing))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Coverage file not found: " + missing);
  }

  @Test
  void aDirectoryIsNotACoverageFile() {
    assertThatThrownBy(() -> CoverageFormat.JACOCO.read(tempDir))
        .hasMessage("Coverage file not found: " + tempDir);
  }

  @Test
  void percentIsFormattedWithTwoDecimalsOrNotAvailable() {
    assertThat(CoverageData.percent(2.0 / 3)).isEqualTo("66.67%");
    assertThat(CoverageData.percent(1.0)).isEqualTo("100.00%");
    assertThat(CoverageData.percent(null)).isEqualTo("N/A");
  }

  /** A method of the given first line and four lines long. */
  private static CognitiveMetrics unit(final String className, final String name, final int line) {
    final CognitiveMetrics metrics = new CognitiveMetrics(className, name, line);
    metrics.setValue(MetricName.LINE_COUNT, 4);
    return metrics;
  }

  private Path write(final String name, final String content) throws Exception {
    return Files.writeString(tempDir.resolve(name), content);
  }
}
