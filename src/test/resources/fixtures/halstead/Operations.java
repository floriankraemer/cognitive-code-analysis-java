package fixtures.halstead;

import java.util.List;

class Operations {

  int count;

  static class Holder {
    int count;
  }

  void empty() {}

  int add(int a, int b) {
    return a + b;
  }

  void assignmentsAndUnary(int a) {
    int x = a;
    x += 1;
    x = -x;
  }

  void chainedCalls(String s) {
    s.trim().length();
  }

  void creation(int n) {
    Object o = new StringBuilder(n);
  }

  int ternaryAndInstanceOf(Object o) {
    return o instanceof String ? 1 : 2;
  }

  void literals() {
    foo("s", 'c', 1, 2L, 1.5, true, null, """
        text""");
  }

  void foo(Object... values) {}

  int repeated(int a) {
    return a + a * a + 1 + 1;
  }

  int fieldAccess(Holder h) {
    return this.count + h.count;
  }

  int anonymousAndLocalClassesAreSkipped(int a) {
    Object o = new Object() {
      public String toString() {
        return "x" + a;
      }
    };
    class Local {
      int f() {
        return a + 1;
      }
    }
    return a;
  }

  void lambdasCount(List<Integer> list) {
    list.forEach(x -> System.out.println(x * 2));
  }
}
