package net.floriankraemer.cognitive_analysis.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.MetricName;

/** The measured counts of one method, their weights and the resulting score. */
public class CognitiveMetrics {

  private final String className;
  private final String methodName;
  private final int line;

  private final Map<MetricName, Integer> values = new EnumMap<>(MetricName.class);
  private final Map<MetricName, Double> weights = new EnumMap<>(MetricName.class);
  private final Map<MetricName, Delta> deltas = new EnumMap<>(MetricName.class);

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
    this.className = className;
    this.methodName = methodName;
    this.line = line;
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
