package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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

  private Path touch(final String name) throws IOException {
    return Files.writeString(directory().resolve(name), "{}");
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
  void ignoresNamesThatAreNoBaselines() throws Exception {
    touch("notes.json");
    touch("baseline-2025-01-01_00-00-00.txt");
    touch("other-2025-01-01_00-00-00.json");
    touch("baseline-manual.json");
    touch("baseline-2025-01-01_00-00-00-x.json");

    assertThat(BaselineFiles.latest(tempDir)).isEmpty();
  }

  @Test
  void ignoresNamesWithAnImpossibleDateOrSuffix() throws Exception {
    touch("baseline-2025-13-01_00-00-00.json");
    touch("baseline-2025-02-30_00-00-00.json");
    touch("baseline-2025-01-01_25-00-00.json");
    touch("baseline-2025-01-01_00-60-00.json");
    touch("baseline-2025-01-01_00-00-00-99999999999.json");
    final Path valid = touch("baseline-2020-01-01_00-00-00.json");

    assertThat(BaselineFiles.latest(tempDir)).contains(valid);
  }

  @Test
  void ignoresDirectoriesThatLookLikeBaselines() throws Exception {
    Files.createDirectories(directory().resolve("baseline-2030-01-01_00-00-00.json"));
    final Path file = touch("baseline-2020-01-01_00-00-00.json");

    assertThat(BaselineFiles.latest(tempDir)).contains(file);
  }

  @Test
  void picksTheNewestTimestampInTheName() throws Exception {
    touch("baseline-2025-01-01_10-00-00.json");
    final Path newest = touch("baseline-2025-01-02_09-00-00.json");
    touch("baseline-2024-12-31_23-59-59.json");

    assertThat(BaselineFiles.latest(tempDir)).contains(newest);
  }

  @Test
  void comparesTheTimestampsToTheSecond() throws Exception {
    touch("baseline-2025-01-01_10-00-00.json");
    final Path newest = touch("baseline-2025-01-01_10-00-01.json");

    assertThat(BaselineFiles.latest(tempDir)).contains(newest);
  }

  @Test
  void theSuffixNumberIsComparedAsANumberAfterTheTimestamp() throws Exception {
    touch("baseline-2025-01-01_10-00-00.json");
    touch("baseline-2025-01-01_10-00-00-2.json");
    final Path newest = touch("baseline-2025-01-01_10-00-00-10.json");
    touch("baseline-2025-01-01_09-59-59-99.json");

    assertThat(BaselineFiles.latest(tempDir)).contains(newest);
  }

  @Test
  void aNewerTimestampBeatsAnOlderOneWithASuffix() throws Exception {
    touch("baseline-2025-01-01_10-00-00-5.json");
    final Path newest = touch("baseline-2025-01-01_10-00-01.json");

    assertThat(BaselineFiles.latest(tempDir)).contains(newest);
  }

  @Test
  void ordersCandidatesByTimestampThenSuffix() {
    final LocalDateTime time = LocalDateTime.parse("2025-01-01T10:00:00");
    final BaselineFiles.Candidate plain = new BaselineFiles.Candidate(Path.of("a"), time, 0);
    final BaselineFiles.Candidate suffixed = new BaselineFiles.Candidate(Path.of("b"), time, 1);
    final BaselineFiles.Candidate later = new BaselineFiles.Candidate(Path.of("c"), time.plusSeconds(1), 0);

    assertThat(BaselineFiles.NEWEST_LAST.compare(plain, suffixed)).isNegative();
    assertThat(BaselineFiles.NEWEST_LAST.compare(suffixed, plain)).isPositive();
    assertThat(BaselineFiles.NEWEST_LAST.compare(suffixed, later)).isNegative();
    assertThat(BaselineFiles.NEWEST_LAST.compare(later, suffixed)).isPositive();
    assertThat(BaselineFiles.NEWEST_LAST.compare(plain, plain)).isZero();
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

  // --- generate ---

  private static List<ScoredMethod> methods() {
    final CognitiveMetrics metrics = new CognitiveMetrics("a.A", "go", 3);
    metrics.setValue(MetricName.IF_COUNT, 2);
    metrics.setWeight(MetricName.IF_COUNT, 1.23456);
    metrics.setCognitiveComplexity(1.235);
    return List.of(new ScoredMethod("src/A.java", "a.A::go", metrics));
  }

  private static Map<MetricName, Double> zeroWeights() {
    final Map<MetricName, Double> weights = new EnumMap<>(MetricName.class);
    for (final MetricName name : MetricName.values()) {
      weights.put(name, 0.0);
    }
    return weights;
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
  void usesUtcForTheNameAndTheCreationTime() throws Exception {
    final Clock plusFive = CLOCK.withZone(ZoneOffset.ofHours(5));

    final Path relative = new BaselineFiles(plusFive).generate(tempDir, TestConfigs.defaults(), methods());

    assertThat(relative.getFileName()).hasToString("baseline-2025-03-04_05-06-07.json");
    assertThat(Files.readString(tempDir.resolve(relative))).contains("\"createdAt\" : \"2025-03-04 05:06:07\"");
  }

  @Test
  void neverReplacesAnExistingFileAndCountsTheSuffixUp() throws Exception {
    final Path first = tempDir.resolve(new BaselineFiles(CLOCK).generate(tempDir, TestConfigs.defaults(), methods()));
    final String content = Files.readString(first);
    Files.writeString(first, "keep me");

    final Path second = new BaselineFiles(CLOCK).generate(tempDir, TestConfigs.defaults(), methods());
    final Path third = new BaselineFiles(CLOCK).generate(tempDir, TestConfigs.defaults(), methods());

    assertThat(Files.readString(first)).isEqualTo("keep me");
    assertThat(second.getFileName()).hasToString("baseline-2025-03-04_05-06-07-1.json");
    assertThat(third.getFileName()).hasToString("baseline-2025-03-04_05-06-07-2.json");
    assertThat(Files.readString(tempDir.resolve(second))).isEqualTo(content);
    assertThat(BaselineFiles.latest(tempDir)).contains(tempDir.resolve(third));
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
        Optional.empty(),
        Map.of("a.A@x/A.java", Map.of("go", before), "a.A@z/A.java", Map.of("go", before), "b.B", Map.of("go", before)));

    BaselineApplier.apply(baseline, List.of(
        new ScoredMethod("x/A.java", "a.A::go", first),
        new ScoredMethod("y/A.java", "a.A::go", second),
        new ScoredMethod("b/B.java", "b.B::go", alone)));

    assertThat(first.delta(MetricName.IF_COUNT)).isPresent();
    assertThat(second.delta(MetricName.IF_COUNT)).isEmpty();
    assertThat(alone.delta(MetricName.IF_COUNT)).isPresent();
  }
}
