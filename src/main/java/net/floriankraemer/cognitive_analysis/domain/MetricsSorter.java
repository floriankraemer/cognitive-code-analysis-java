package net.floriankraemer.cognitive_analysis.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.MetricName;

/**
 * Orders methods by one field. Numbers are compared numerically, text ignoring the letter case. Methods with
 * an equal value stay in ascending order of class and then method, in either direction.
 */
public final class MetricsSorter {

  private static final Map<String, Comparator<ScoredMethod>> COMPARATORS = comparators();

  /** The names accepted by {@code --sort-by}, in the order to list them. */
  public static final List<String> FIELDS = List.copyOf(COMPARATORS.keySet());

  private static final Comparator<ScoredMethod> TIE_BREAK =
      text(method -> method.metrics().getClassName()).thenComparing(text(method -> method.metrics().getMethodName()));

  private final Comparator<ScoredMethod> comparator;

  /**
   * @param field one of {@link #FIELDS}, or null to keep the given order
   * @param order {@code asc} or {@code desc}, in any letter case
   * @throws CognitiveAnalysisException for an unknown field or order; the order is checked without a field too
   */
  public MetricsSorter(final String field, final String order) throws CognitiveAnalysisException {
    if (field != null && !COMPARATORS.containsKey(field)) {
      throw new CognitiveAnalysisException(
          "Invalid sort field \"" + field + "\". Available fields: " + String.join(", ", FIELDS));
    }
    final boolean descending = isDescending(order);
    if (field == null) {
      comparator = null;
      return;
    }
    final Comparator<ScoredMethod> primary = COMPARATORS.get(field);
    comparator = (descending ? primary.reversed() : primary).thenComparing(TIE_BREAK);
  }

  /** @return a new list in the requested order; the given list itself when there is no field to sort by */
  public List<ScoredMethod> sort(final List<ScoredMethod> methods) {
    if (comparator == null) {
      return methods;
    }
    final List<ScoredMethod> sorted = new ArrayList<>(methods);
    sorted.sort(comparator);
    return sorted;
  }

  private static boolean isDescending(final String order) throws CognitiveAnalysisException {
    final String normalized = order.toLowerCase(Locale.ROOT);
    if (!normalized.equals("asc") && !normalized.equals("desc")) {
      throw new CognitiveAnalysisException("Sort order must be \"asc\" or \"desc\", got \"" + order + "\"");
    }
    return normalized.equals("desc");
  }

  private static Map<String, Comparator<ScoredMethod>> comparators() {
    final Map<String, Comparator<ScoredMethod>> fields = new LinkedHashMap<>();
    fields.put("score", numeric(method -> method.metrics().getCognitiveComplexity()));
    fields.put("halstead", numeric(method -> method.metrics().getHalstead().volume()));
    fields.put("cyclomatic", numeric(method -> method.metrics().getCyclomatic().complexity()));
    fields.put("understandability", numeric(method -> method.metrics().getUnderstandability().complexity()));
    fields.put("class", text(method -> method.metrics().getClassName()));
    fields.put("method", text(method -> method.metrics().getMethodName()));
    fields.put("file", text(ScoredMethod::file));
    for (final MetricName name : MetricName.values()) {
      fields.put(name.key(), numeric(method -> method.metrics().value(name)));
    }
    for (final MetricName name : MetricName.values()) {
      fields.put(name.key() + "Weight", numeric(method -> method.metrics().weight(name)));
    }
    return fields;
  }

  private static Comparator<ScoredMethod> numeric(final ToDoubleFunction<ScoredMethod> value) {
    return Comparator.comparingDouble(value);
  }

  private static Comparator<ScoredMethod> text(final Function<ScoredMethod, String> value) {
    return Comparator.comparing(value, String.CASE_INSENSITIVE_ORDER);
  }
}
