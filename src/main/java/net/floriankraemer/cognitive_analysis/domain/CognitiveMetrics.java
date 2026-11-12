package net.floriankraemer.cognitive_analysis.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.MetricName;

/** The measured counts of one method, their weights and the resulting score. */
public class CognitiveMetrics {

  private final String className;
  private final String methodName;
  private final int line;

  @JsonProperty("values")
  private final Map<MetricName, Integer> values;

  @JsonProperty("weights")
  private final Map<MetricName, Double> weights;

  @JsonProperty("deltas")
  private final Map<MetricName, Delta> deltas;

  @JsonProperty("otherDeltas")
  private final Map<DeltaMetric, Delta> otherDeltas;

  @JsonProperty("cognitiveComplexity")
  private double cognitiveComplexity;
  private CyclomaticComplexity cyclomatic = new CyclomaticVisitor().result();
  private HalsteadMetrics halstead = new HalsteadMetrics(0, 0, 0, 0);
  private Understandability understandability = Understandability.NONE;

  /**
   * @param className the fully qualified class name, nested classes separated by dots
   * @param methodName the method name, with the parameter types when the method is overloaded
   * @param line the line the declaration starts on
   */
  public CognitiveMetrics(final String className, final String methodName, final int line) {
    this(className, methodName, line, Map.of(), Map.of(), Map.of(), Map.of(), 0.0);
  }

  /** Restores a state written by Jackson, see the annotated fields. */
  @JsonCreator
  CognitiveMetrics(
      @JsonProperty("className") final String className,
      @JsonProperty("methodName") final String methodName,
      @JsonProperty("line") final int line,
      @JsonProperty("values") final Map<MetricName, Integer> values,
      @JsonProperty("weights") final Map<MetricName, Double> weights,
      @JsonProperty("deltas") final Map<MetricName, Delta> deltas,
      @JsonProperty("otherDeltas") final Map<DeltaMetric, Delta> otherDeltas,
      @JsonProperty("cognitiveComplexity") final double cognitiveComplexity) {
    this.className = className;
    this.methodName = methodName;
    this.line = line;
    this.values = new EnumMap<>(MetricName.class);
    this.values.putAll(values);
    this.weights = new EnumMap<>(MetricName.class);
    this.weights.putAll(weights);
    this.deltas = new EnumMap<>(MetricName.class);
    this.deltas.putAll(deltas);
    this.otherDeltas = new EnumMap<>(DeltaMetric.class);
    if (otherDeltas != null) {
      this.otherDeltas.putAll(otherDeltas);
    }
    this.cognitiveComplexity = cognitiveComplexity;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public int getLine() {
    return line;
  }

  /** The raw count of a metric, 0 until set. */
  public int value(final MetricName name) {
    return values.getOrDefault(name, 0);
  }

  public void setValue(final MetricName name, final int value) {
    values.put(name, value);
  }

  /** The weight of a metric in the score, 0 until calculated. */
  public double weight(final MetricName name) {
    return weights.getOrDefault(name, 0.0);
  }

  public void setWeight(final MetricName name, final double weight) {
    weights.put(name, weight);
  }

  /** The change of the weight against a previous run, empty until {@link #applyBaseline} ran. */
  public Optional<Delta> delta(final MetricName name) {
    return Optional.ofNullable(deltas.get(name));
  }

  /** The change of a tracked metric against the baseline, empty when the baseline has no value for it. */
  public Optional<Delta> otherDelta(final DeltaMetric metric) {
    return Optional.ofNullable(otherDeltas.get(metric));
  }

  /** Sets every delta that the baseline method has a value to compare with. */
  public void applyBaseline(final BaselineMethod baseline) {
    for (final MetricName name : MetricName.values()) {
      deltas.put(name, Delta.rounded(baseline.weight(name), weight(name)));
    }
    for (final DeltaMetric metric : DeltaMetric.values()) {
      baseline.other(metric).ifPresent(before -> otherDeltas.put(metric, Delta.rounded(before, metric.valueOf(this))));
    }
  }

  public double getCognitiveComplexity() {
    return cognitiveComplexity;
  }

  public void setCognitiveComplexity(final double cognitiveComplexity) {
    this.cognitiveComplexity = cognitiveComplexity;
  }

  /** The cyclomatic complexity, 1 until set. */
  public CyclomaticComplexity getCyclomatic() {
    return cyclomatic;
  }

  public void setCyclomatic(final CyclomaticComplexity cyclomatic) {
    this.cyclomatic = cyclomatic;
  }

  /** The Halstead metrics, all 0 until set. */
  public HalsteadMetrics getHalstead() {
    return halstead;
  }

  public void setHalstead(final HalsteadMetrics halstead) {
    this.halstead = halstead;
  }

  /** The understandability (Sonar cognitive complexity), 0 until set. */
  public Understandability getUnderstandability() {
    return understandability;
  }

  public void setUnderstandability(final Understandability understandability) {
    this.understandability = understandability;
  }
}
