package fixtures.cognitive;

import java.util.List;

class Variables {

  private int count;
  private Variables next;
  private static int TOTAL;
  private int[] values;

  int distinctNames(int parameter) {
    int a = parameter;
    int b = a + 1;
    a = b * 2;
    for (int i = 0; i < b; i++) {
      a += i;
    }
    for (int i = 0; i < a; i++) {
      b += i;
    }
    return a + b;
  }

  void lambdaCatchAndForEachVariables(List<String> items) {
    items.forEach(item -> System.out.println(item));
    items.forEach((String item) -> System.out.println(item));
    try {
      items.clear();
    } catch (IllegalStateException | UnsupportedOperationException e) {
      throw e;
    }
    try (var stream = items.stream()) {
      stream.count();
    } catch (RuntimeException e) {
      throw e;
    }
    for (String each : items) {
      each.length();
    }
  }

  int fieldAccess(Variables other) {
    this.count = other.count + next.count;
    int length = this.values.length;
    TOTAL = Variables.TOTAL + Math.abs(count);
    return other.next.next.count + length + java.util.Objects.hash(1);
  }

  void thisCountsOnce() {
    this.count = 1;
    this.count++;
    Variables.this.count--;
    this.helper(this);
  }

  int shadowsField() {
    int count = this.count;
    return count;
  }

  private void helper(Variables variables) {
  }
}
