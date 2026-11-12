# CI

The tool fits into a pipeline in three ways: it annotates or reports the methods above the threshold, it gates the build, and it compares each run with a committed baseline.
`analyse` exits with status 0 when violations are found, so the gate is a separate step that reads the report.

The examples build the tool from its repository, because no released jar is published yet.
Replace the build step with a download of a released jar once one is available.

## GitHub Actions

The workflow below runs on pull requests.
It writes a SARIF file for code scanning, prints one annotation per violation in the log, and fails the job when a method is above the threshold.

```yaml
name: Cognitive complexity

on:
  pull_request:

permissions:
  contents: read
  security-events: write

jobs:
  cognitive:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: temurin

      - name: Build cognitive-analysis
        run: |
          git clone --depth 1 https://github.com/floriankraemer/cognitive-code-analysis-java.git "$RUNNER_TEMP/jcca"
          (cd "$RUNNER_TEMP/jcca" && ./mvnw -B -q package -DskipTests)

      - name: Analyse
        run: |
          java -jar "$RUNNER_TEMP/jcca/target/cognitive-analysis.jar" analyse src/main/java -r sarif -f cognitive.sarif
          java -jar "$RUNNER_TEMP/jcca/target/cognitive-analysis.jar" analyse src/main/java -r github-actions -f cognitive-annotations.txt
          cat cognitive-annotations.txt
          test ! -s cognitive-annotations.txt

      - name: Upload SARIF to code scanning
        if: always()
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: cognitive.sarif
```

The `github-actions` report is empty when no method is above the threshold, so `test ! -s` fails only when there is a violation.
The `cat` runs first, so the annotations are printed even when the gate fails.
The `if: always()` on the upload makes the findings visible in the security tab even when the gate fails.

The paths in the annotations are relative to the repository root, because the job runs there.

## GitLab CI

The job below writes a Code Quality report, which GitLab shows in merge requests, and fails when a method is above the threshold.

```yaml
cognitive-complexity:
  image: eclipse-temurin:21-jdk
  stage: test
  before_script:
    - apt-get update -qq && apt-get install -y -qq git
    - git clone --depth 1 https://github.com/floriankraemer/cognitive-code-analysis-java.git /tmp/jcca
    - (cd /tmp/jcca && ./mvnw -B -q package -DskipTests)
  script:
    - java -jar /tmp/jcca/target/cognitive-analysis.jar analyse src/main/java -r gitlab-codequality -f gl-code-quality-report.json
    - test "$(tr -d '[:space:]' < gl-code-quality-report.json)" = "[]"
  artifacts:
    when: always
    reports:
      codequality: gl-code-quality-report.json
```

The report is an empty array, written as `[ ]`, when no method is above the threshold.
The gate compares the report with `[]` after removing the whitespace.
`artifacts: when: always` keeps the report when the gate fails, so the merge request still shows the issues.

## Churn in a pipeline

`churn` needs the git history, so the checkout must not be shallow.
Set `fetch-depth: 0` in GitHub Actions, or `GIT_DEPTH: 0` in GitLab CI, and run the command in the job after the checkout.

```bash
java -jar target/cognitive-analysis.jar churn src/main/java --since 2024-01-01 -r json -f churn.json
```

The churn report is informative rather than a gate, so it usually runs in a scheduled job and is kept as an artifact.

## Baseline workflow

A baseline records the metrics of a known state.
Later runs print the change of each metric against it, so a reviewer sees the effect of a merge request without any gate.

1. On the default branch, generate a baseline and commit it:

   ```bash
   java -jar target/cognitive-analysis.jar analyse src/main/java -g
   git add .jcca/baseline
   git commit -m "chore: update cognitive complexity baseline"
   ```

2. In the merge request pipeline, the tool uses the newest file in `.jcca/baseline` automatically.
   The console log of the job shows the deltas, marked `Δ +` in red and `Δ -` in green on a terminal.

   ```bash
   java -jar target/cognitive-analysis.jar analyse src/main/java
   ```

3. Generate a new baseline when the team accepts a change, so the next comparison starts from there.

The deltas are shown in the console and the Markdown, CSV, HTML and JSON reports, see [Baseline](./Baseline.md#deltas).
The issue formats do not carry deltas.

Use `-b` to compare with a specific file instead of the newest one, for example in a release branch:

```bash
java -jar target/cognitive-analysis.jar analyse src/main/java -b .jcca/baseline/baseline-2026-11-12_21-35-02.json
```
