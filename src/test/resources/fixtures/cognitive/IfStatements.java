package fixtures.cognitive;

class IfStatements {

  int chain(int a) {
    if (a > 3) {
      return 3;
    } else if (a > 2) {
      return 2;
    } else if (a > 1) {
      return 1;
    } else {
      return 0;
    }
  }

  void nestedInThenAndElse(boolean a, boolean b, boolean c) {
    if (a) {
      if (b) {
        if (c) {
          log("abc");
        }
      }
    } else {
      if (b) {
        log("b");
      } else {
        log("none");
      }
    }
  }

  void elseIfBodyNests(int a) {
    if (a == 1) {
      log("1");
    } else if (a == 2) {
      if (a > 0) {
        log("2");
      }
    }
  }

  void braceElseIf(int a) {
    if (a == 1) {
      log("1");
    } else {
      if (a == 2) {
        log("2");
      }
    }
  }

  void ifsInLoopsTrySwitchAndLambda(java.util.List<Integer> values) {
    for (int value : values) {
      if (value > 0) {
        log("positive");
      }
    }
    while (values.isEmpty()) {
      if (values.size() == 0) {
        break;
      }
    }
    try {
      if (values.contains(1)) {
        log("one");
      }
    } catch (RuntimeException e) {
      if (e.getMessage() != null) {
        log(e.getMessage());
      }
    } finally {
      if (values.contains(2)) {
        log("two");
      }
    }
    switch (values.size()) {
      case 1:
        if (values.get(0) == 1) {
          log("single");
        }
        break;
      default:
        log("other");
    }
    values.forEach(v -> {
      if (v > 1) {
        log("big");
      }
    });
  }

  int ternariesDoNotCount(int a) {
    int b = a > 0 ? 1 : 2;
    return a > 1 ? b : (a > 2 ? 3 : 4);
  }

  void ifInsideLambdaInsideIf(java.util.List<Integer> values) {
    if (!values.isEmpty()) {
      values.forEach(v -> {
        if (v > 0) {
          log("x");
        }
      });
    }
  }

  private void log(String message) {
    System.out.println(message);
  }
}
