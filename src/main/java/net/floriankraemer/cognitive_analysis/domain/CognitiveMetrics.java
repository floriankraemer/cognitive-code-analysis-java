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

  @JsonProperty("cognitiveComplexity")
  private double cognitiveComplexity;

  /**
   * @param className the fully qualified class name, nested classes separated by dots
   * @param methodName the method name, with the parameter types when the method is overloaded
   * @param line the line the declaration starts on
   */
  public CognitiveMetrics(final String className, final String methodName, final int line) {
    this(className, methodName, line, Map.of(), Map.of(), Map.of(), 0.0);
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

  /** The change of the weight against a previous run, empty until {@link #calculateDeltas} ran. */
  public Optional<Delta> delta(final MetricName name) {
    return Optional.ofNullable(deltas.get(name));
  }

  public void calculateDeltas(final CognitiveMetrics previous) {
    for (final MetricName name : MetricName.values()) {
      deltas.put(name, new Delta(previous.weight(name), weight(name)));
    }
  }

  public double getCognitiveComplexity() {
    return cognitiveComplexity;
  }

  public void setCognitiveComplexity(final double cognitiveComplexity) {
    this.cognitiveComplexity = cognitiveComplexity;
  }
}
