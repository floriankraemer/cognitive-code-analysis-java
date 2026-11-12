package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricsCollectionTest {

  private final CognitiveMetrics one = new CognitiveMetrics("p.A", "one", 1);
  private final CognitiveMetrics two = new CognitiveMetrics("p.A", "two", 2);
  private final CognitiveMetrics three = new CognitiveMetrics("p.B", "three", 3);

  @Test
  void listsTheMethodsFileByFileInCollectionOrder() {
    MetricsCollection collection = new MetricsCollection();
    collection.put("A.java", Map.of("p.A::one", one));
    collection.put("B.java", new LinkedHashMap<>(Map.of("p.B::three", three)));

    assertThat(collection.methods()).containsExactly(
        new ScoredMethod("A.java", "p.A::one", one), new ScoredMethod("B.java", "p.B::three", three));
  }

  @Test
  void isEmptyWithoutMethods() {
    assertThat(new MetricsCollection().methods()).isEmpty();
    assertThat(MetricsCollection.of(List.of())).isEmpty();
  }

  @Test
  void groupsMethodsByFileInTheOrderOfTheirFirstAppearance() {
    MetricsCollection collection = MetricsCollection.of(List.of(
        new ScoredMethod("B.java", "p.B::three", three),
        new ScoredMethod("A.java", "p.A::two", two),
        new ScoredMethod("B.java", "p.B::more", three),
        new ScoredMethod("A.java", "p.A::one", one)));

    assertThat(collection.keySet()).containsExactly("B.java", "A.java");
    assertThat(collection.get("B.java").keySet()).containsExactly("p.B::three", "p.B::more");
    assertThat(collection.get("A.java").keySet()).containsExactly("p.A::two", "p.A::one");
  }
}
