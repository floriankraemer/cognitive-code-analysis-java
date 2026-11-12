package net.floriankraemer.cognitive_analysis.coverage;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/** The line coverage of the classes and methods of one coverage report, keyed by the Java class name. */
public final class CoverageData {

  /**
   * @param name the method name as the report names it, {@code <init>} for constructors
   * @param firstLine the first line of the method body in the report, 0 when the report has none
   * @param rate the line coverage between 0.0 and 1.0, null when the method has no lines
   */
  record MethodCoverage(String name, int firstLine, Double rate) {}

  /** @param rate the line coverage between 0.0 and 1.0, null when the class has no lines */
  record ClassCoverage(Double rate, List<MethodCoverage> methods) {}

  private static final String CONSTRUCTOR = "<init>";

  /** Suffixes of compiler-generated methods, which are no module names. */
  private static final List<String> SYNTHETIC_PREFIXES = List.of("lambda", "default", "access");

  private final Map<String, ClassCoverage> classes;

  /** @param classes by fully qualified class name, nested classes separated by dots */
  CoverageData(final Map<String, ClassCoverage> classes) {
    this.classes = classes;
  }

  /**
   * A warning when the report knows none or only some of the classes of the given methods; empty when it knows
   * all of them or there are no methods.
   */
  public Optional<String> matchWarning(final String file, final Iterable<ScoredMethod> methods) {
    final Set<String> analysed = new HashSet<>();
    methods.forEach(method -> analysed.add(method.metrics().getClassName()));
    final long matched = analysed.stream().filter(classes::containsKey).count();
    if (analysed.isEmpty() || matched == analysed.size()) {
      return Optional.empty();
    }
    final String matching = matched == 0 ? "none of the " + analysed.size() : matched + " of " + analysed.size();
    return Optional.of("Coverage report " + file + " matched " + matching + " analysed classes.");
  }

  /** Sets the coverage of each method, to empty for those the report has no data on. */
  public void applyTo(final Iterable<ScoredMethod> methods) {
    for (final ScoredMethod method : methods) {
      method.metrics().setCoverage(methodCoverage(method.metrics()).orElse(null));
    }
  }

  /** A coverage rate between 0.0 and 1.0 as {@code 83.33%}, or {@code N/A} for none. */
  public static String percent(final Double rate) {
    return rate == null ? "N/A" : String.format(Locale.ROOT, "%.2f%%", rate * 100);
  }

  /** {@code p/Outer$Inner} (JaCoCo, Cobertura) as {@code p.Outer.Inner}, the name the analysis uses. */
  static String javaName(final String binaryName) {
    return binaryName.replace('/', '.').replace('$', '.');
  }

  /** The line coverage of the class, empty when the report does not know it or it has no lines. */
  public Optional<Double> classCoverage(final String className) {
    return Optional.ofNullable(classes.get(className)).map(ClassCoverage::rate);
  }

  /**
   * The line coverage of a method: that of the method in the report with the same name whose first line
   * lies inside the lines of the analysed method, else that of its class, else empty.
   */
  public Optional<Double> methodCoverage(final CognitiveMetrics method) {
    final ClassCoverage coveredClass = classes.get(method.getClassName());
    if (coveredClass == null) {
      return Optional.empty();
    }
    final String name = reportName(method);
    final List<MethodCoverage> sameName = candidates(coveredClass.methods(), name);
    final int from = method.getLine();
    final int to = from + method.value(MetricName.LINE_COUNT);
    return sameName.stream()
        .filter(covered -> covered.rate() != null && isAt(covered, sameName.size(), from, to))
        .findFirst()
        .map(MethodCoverage::rate)
        .or(() -> classCoverage(method.getClassName()));
  }

  /**
   * The methods of the report with the name, or else those that Kotlin compiled from an internal function of
   * that name.
   */
  private static List<MethodCoverage> candidates(final List<MethodCoverage> methods, final String name) {
    final List<MethodCoverage> exact = methods.stream().filter(covered -> covered.name().equals(name)).toList();
    return exact.isEmpty()
        ? methods.stream().filter(covered -> isInternalFunction(covered.name(), name)).toList()
        : exact;
  }

  /**
   * True for {@code audit$app}, the name Kotlin gives an {@code internal fun audit()} of the module {@code app}.
   * Synthetic names such as {@code run$lambda$0}, {@code run$default}, {@code access$run} or {@code run$1} are not.
   */
  static boolean isInternalFunction(final String reportName, final String name) {
    if (!reportName.startsWith(name + "$")) {
      return false;
    }
    final String module = reportName.substring(name.length() + 1);
    return !module.isEmpty()
        && module.indexOf('$') < 0
        && !Character.isDigit(module.charAt(0))
        && SYNTHETIC_PREFIXES.stream().noneMatch(module::startsWith);
  }

  /** A method without lines in the report is only taken when no other method has its name. */
  private static boolean isAt(final MethodCoverage covered, final int sameNameCount, final int from, final int to) {
    return covered.firstLine() == 0 ? sameNameCount == 1 : covered.firstLine() >= from && covered.firstLine() < to;
  }

  /** Our name of an overload carries the parameter types, {@code run(int)}; constructors are named after the class. */
  private static String reportName(final CognitiveMetrics method) {
    final String name = method.getMethodName();
    final int parameters = name.indexOf('(');
    final String base = parameters < 0 ? name : name.substring(0, parameters);
    final String simpleClassName = method.getClassName().substring(method.getClassName().lastIndexOf('.') + 1);
    return base.equals(simpleClassName) ? CONSTRUCTOR : base;
  }
}
