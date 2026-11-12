package fixtures.cyclomatic;

import java.util.List;

class Decisions {

  void empty() {}

  void ifOnly(int a) {
    if (a > 0) {}
  }

  void elseDoesNotCount(boolean a) {
    if (a) {} else {}
  }

  void elseIfChain(int a) {
    if (a > 0) {} else if (a < 0) {} else {}
  }

  void switchWithDefault(int a) {
    switch (a) {
      case 1: break;
      case 2: break;
      default: break;
    }
  }

  void switchMultiLabelArrow(int a) {
    switch (a) {
      case 1, 2, 3 -> {}
      case 4 -> {}
      default -> {}
    }
  }

  String switchExpression(int a) {
    return switch (a) {
      case 1 -> "a";
      case 2, 3 -> "b";
      default -> "c";
    };
  }

  void loops(List<String> list) {
    while (list.isEmpty()) {}
    do {} while (list.isEmpty());
    for (int i = 0; i < 1; i++) {}
    for (String s : list) {}
  }

  void catches() {
    try {
    } catch (IllegalStateException e) {
    } catch (IllegalArgumentException | NullPointerException e) {
    } finally {
    }
  }

  boolean logical(boolean a, boolean b, boolean c) {
    return a && b || c;
  }

  int ternary(int a) {
    return a > 0 ? 1 : 2;
  }

  int nestedTernary(int a) {
    return a > 0 ? a > 1 ? 1 : 2 : 3;
  }

  void lambdasCount(List<Integer> list) {
    list.forEach(x -> {
      if (x > 0 && x < 5) {}
    });
    Runnable outer = () -> {
      Runnable inner = () -> {
        if (list.isEmpty()) {}
      };
    };
  }

  void anonymousAndLocalClassesAreSkipped(boolean a) {
    Object o = new Object() {
      public String toString() {
        if (a) {}
        return "";
      }
    };
    class Local {
      void m() {
        if (a) {}
      }
    }
    if (a) {}
  }

  void creationArgumentsCount(boolean a) {
    new StringBuilder(a ? "x" : "y");
  }

  void everything(boolean a, List<String> list) {
    if (a && list.isEmpty() || !a) {}
    while (a) {}
    do {} while (a);
    for (;;) { break; }
    for (String s : list) {}
    try {} catch (RuntimeException e) {}
    switch (list.size()) {
      case 0: break;
      default: break;
    }
    boolean t = a ? true : false;
  }

  void guardedPattern(Object o, boolean a, boolean b) {
    switch (o) {
      case String s when a && b -> {}
      default -> {}
    }
  }

  void nullAndDefault(Object o) {
    switch (o) {
      case null, default -> {}
    }
  }

  void switchInLambda(int a) {
    Runnable r = () -> {
      switch (a) {
        case 1 -> {}
        case 2 -> {}
        default -> {}
      }
    };
  }

  void doWhileBodyCounts(boolean a) {
    do {
      if (a) {}
    } while (a);
  }
}
