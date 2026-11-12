package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.domain.Baseline;
import net.floriankraemer.cognitive_analysis.domain.BaselineMethod;
import net.floriankraemer.cognitive_analysis.domain.BaselineReader;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.DeltaMetric;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaselineFilesTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-03-04T05:06:07Z"), ZoneOffset.UTC);

  @TempDir
  Path tempDir;

  private Path directory() throws IOException {
    return Files.createDirectories(tempDir.resolve(".jcca/baseline"));
  }

  private Path touch(final String name, final long modifiedMillis) throws IOException {
    final Path file = Files.writeString(directory().resolve(name), "{}");
    Files.setLastModifiedTime(file, FileTime.fromMillis(modifiedMillis));
    return file;
  }

  // --- latest ---

  @Test
  void findsNothingWithoutTheDirectory() throws Exception {
    assertThat(BaselineFiles.latest(tempDir)).isEmpty();
  }

  @Test
  void findsNothingInAnEmptyDirectory() throws Exception {
    directory();

    assertThat(BaselineFiles.latest(tempDir)).isEmpty();
  }

  @Test
  void findsNothingWhenTheDirectoryIsAFile() throws Exception {
    Files.createDirectories(tempDir.resolve(".jcca"));
    Files.writeString(tempDir.resolve(".jcca/baseline"), "x");

    assertThat(BaselineFiles.latest(tempDir)).isEmpty();
  }

  @Test
  void ignoresFilesThatAreNoBaselines() throws Exception {
    touch("notes.json", 1);
    touch("baseline-2025-01-01_00-00-00.txt", 1);
    touch("other-2025-01-01_00-00-00.json", 1);

    assertThat(BaselineFiles.latest(tempDir)).isEmpty();
  }

  @Test
  void picksTheNewestTimestampInTheNameWhateverTheModificationTimes() throws Exception {
    touch("baseline-2025-01-01_10-00-00.json", 9_000);
    final Path newest = touch("baseline-2025-01-02_09-00-00.json", 1_000);
    touch("baseline-2024-12-31_23-59-59.json", 8_000);

    assertThat(BaselineFiles.latest(tempDir)).contains(newest);
  }

  @Test
  void comparesTheTimestampsToTheSecond() throws Exception {
    touch("baseline-2025-01-01_10-00-00.json", 1);
    final Path newest = touch("baseline-2025-01-01_10-00-01.json", 1);

    assertThat(BaselineFiles.latest(tempDir)).contains(newest);
  }

  @Test
  void aNameWithoutTimestampLosesAgainstOneWithTimestamp() throws Exception {
    touch("baseline-manual.json", 9_000);
    final Path stamped = touch("baseline-2020-01-01_00-00-00.json", 1_000);

    assertThat(BaselineFiles.latest(tempDir)).contains(stamped);
  }

  @Test
  void namesWithoutTimestampAreOrderedByModificationTime() throws Exception {
    touch("baseline-a.json", 1_000);
    final Path newer = touch("baseline-b.json", 2_000);
    touch("baseline-c.json", 500);

    assertThat(BaselineFiles.latest(tempDir)).contains(newer);
  }

  @Test
  void equalTimestampsAndModificationTimesAreOrderedByName() {
    final FileTime time = FileTime.fromMillis(1_000);
    final BaselineFiles.Candidate x = new BaselineFiles.Candidate(Path.of("baseline-x.json"), "", time);
    final BaselineFiles.Candidate y = new BaselineFiles.Candidate(Path.of("baseline-y.json"), "", time);

    assertThat(BaselineFiles.NEWEST_LAST.compare(x, y)).isNegative();
    assertThat(BaselineFiles.NEWEST_LAST.compare(y, x)).isPositive();
    assertThat(BaselineFiles.NEWEST_LAST.compare(x, x)).isZero();
  }

  @Test
  void reportsAnUnreadableDirectory() throws Exception {
    final Path directory = directory();
    assumeTrue(directory.getFileSystem().supportedFileAttributeViews().contains("posix"));
    Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("---------"));
    try {
      assumeTrue(!Files.isReadable(directory), "running as a user that ignores permissions");

      assertThatThrownBy(() -> BaselineFiles.latest(tempDir))
          .isInstanceOf(CognitiveAnalysisException.class)
          .hasMessageStartingWith("Cannot read baseline directory " + directory);
    } finally {
      Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
  }

  private static Map<MetricName, Double> zeroWeights() {
    final Map<MetricName, Double> weights = new EnumMap<>(MetricName.class);
    for (final MetricName name : MetricName.values()) {
      weights.put(name, 0.0);
    }
    return weights;
  }

  // --- generate ---

  private static List<ScoredMethod> methods() {
    final CognitiveMetrics metrics = new CognitiveMetrics("a.A", "go", 3);
    metrics.setValue(MetricName.IF_COUNT, 2);
    metrics.setWeight(MetricName.IF_COUNT, 1.23456);
    metrics.setCognitiveComplexity(1.235);
    return List.of(new ScoredMethod("src/A.java", "a.A::go", metrics));
  }

  @Test
  void writesTheFileNamedAfterTheClockAndReturnsTheRelativePath() throws Exception {
    final Path relative = new BaselineFiles(CLOCK).generate(tempDir, TestConfigs.defaults(), methods());

    assertThat(relative).isEqualTo(Path.of(".jcca/baseline/baseline-2025-03-04_05-06-07.json"));
    final String content = Files.readString(tempDir.resolve(relative));
    assertThat(content)
        .startsWith("{\n  \"version\" : \"2.0\",\n  \"createdAt\" : \"2025-03-04 05:06:07\",\n  \"configHash\" : \""
            + Baseline.configHashOf(TestConfigs.defaults()) + "\",\n  \"metrics\" : {\n    \"a.A\" : {")
        .contains("\"ifCountWeight\" : 1.235")
        .endsWith("}\n");
  }

  @Test
  void theGeneratedFileIsReadBackAsABaseline() throws Exception {
    final Path relative = new BaselineFiles(CLOCK).generate(tempDir, TestConfigs.defaults(), methods());

    final Baseline baseline = BaselineReader.read(tempDir.resolve(relative));

    assertThat(baseline.configHash()).contains(Baseline.configHashOf(TestConfigs.defaults()));
    final BaselineMethod method = baseline.method("a.A", "go").orElseThrow();
    assertThat(method.weight(MetricName.IF_COUNT)).isEqualTo(1.235);
    assertThat(method.other(DeltaMetric.SCORE)).contains(1.235);
  }

  @Test
  void omitsTheDeltasOfAnAppliedBaseline() throws Exception {
    final List<ScoredMethod> methods = methods();
    methods.get(0).metrics().applyBaseline(new BaselineMethod(zeroWeights(), Map.of(DeltaMetric.SCORE, 0.0)));

    final Path relative = new BaselineFiles(CLOCK).generate(tempDir, TestConfigs.defaults(), methods);

    assertThat(Files.readString(tempDir.resolve(relative))).doesNotContain("deltas");
  }

  @Test
  void failsWhenTheDirectoryCannotBeCreated() throws Exception {
    Files.writeString(tempDir.resolve(".jcca"), "a file, not a directory");

    assertThatThrownBy(() -> new BaselineFiles(CLOCK).generate(tempDir, TestConfigs.defaults(), methods()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Unable to create directory " + tempDir.resolve(".jcca/baseline"));
  }

  // --- apply ---

  @Test
  void appliesTheBaselineByClassKeyIncludingTheFileOfAClassInSeveralFiles() {
    final CognitiveMetrics first = new CognitiveMetrics("a.A", "go", 1);
    final CognitiveMetrics second = new CognitiveMetrics("a.A", "go", 1);
    first.setWeight(MetricName.IF_COUNT, 2);
    second.setWeight(MetricName.IF_COUNT, 2);
    final CognitiveMetrics alone = new CognitiveMetrics("b.B", "go", 1);
    final BaselineMethod before = new BaselineMethod(zeroWeights(), Map.of());
    final Baseline baseline = new Baseline(
        Optional.empty(), Map.of("a.A@x/A.java", Map.of("go", before), "b.B", Map.of("go", before)));

    BaselineApplier.apply(baseline, List.of(
        new ScoredMethod("x/A.java", "a.A::go", first),
        new ScoredMethod("y/A.java", "a.A::go", second),
        new ScoredMethod("b/B.java", "b.B::go", alone)));

    assertThat(first.delta(MetricName.IF_COUNT)).isPresent();
    assertThat(second.delta(MetricName.IF_COUNT)).isEmpty();
    assertThat(alone.delta(MetricName.IF_COUNT)).isPresent();
  }
}
