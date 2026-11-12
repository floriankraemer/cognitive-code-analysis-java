package fixtures.understandability;

import java.util.List;

class Rules {

  Rules(int a) {
    if (a > 0) {}
    Rules(a);
  }

  void empty() {}

  void ifOnly(boolean a) {
    if (a) {}
  }

  void ifElse(boolean a) {
    if (a) {} else {}
  }

  void elseIfChainIsNotNestedDeeper(int a) {
    if (a > 0) {
      if (a > 1) {}
    } else if (a < 0) {
      if (a < -1) {}
    } else if (a == 0) {
      if (a == 0) {}
    } else {
      if (a == 0) {}
    }
  }

  void elseBodyNests(boolean a, boolean b) {
    if (a) {} else {
      while (b) {}
    }
  }

  int ternary(boolean a) {
    return a ? 1 : 0;
  }

  int nestedTernary(boolean a, boolean b) {
    return a ? (b ? 1 : 2) : 0;
  }

  int ternaryInConditionIsNotNested(boolean a, boolean b) {
    if (a ? b : !b) {
      return 1;
    }
    return 0;
  }

  void switchCountsOnceNotPerCase(int a) {
    switch (a) {
      case 1, 2 -> {}
      case 3 -> {}
      default -> {}
    }
  }

  int switchExpressionNestsItsArms(int a, boolean b) {
    return switch (a) {
      case 1 -> b ? 1 : 2;
      default -> 0;
    };
  }

  void loops(List<String> items, boolean b) {
    for (int i = 0; i < 1; i++) {}
    for (String item : items) {}
    while (b) {}
    do {} while (b);
  }

  void nestedLoops(List<String> items, boolean b) {
    for (String item : items) {
      while (b) {
        do {} while (b);
      }
    }
  }

  void doWhileBodyNestsButConditionDoesNot(boolean a, boolean b) {
    do {
      if (a) {}
    } while (a && b);
  }

  void tryAndFinallyDoNotCount() {
    try {
      run();
    } catch (IllegalStateException | IllegalArgumentException e) {
      if (e != null) {}
    } catch (RuntimeException e) {
    } finally {
      run();
    }
  }

  void lambdaNests(List<String> items) {
    items.forEach(item -> {
      if (item.isEmpty()) {}
    });
  }

  void lambdaInLambdaNestsTwice(List<List<String>> items) {
    items.forEach(inner -> inner.forEach(item -> {
      if (item.isEmpty()) {}
    }));
  }

  void lambdaInsideIfNests(boolean a, List<String> items) {
    if (a) {
      items.forEach(item -> {
        if (item.isEmpty()) {}
      });
    }
  }

  void anonymousAndLocalClassesAreSkipped(boolean a) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        if (a) {}
      }
    };
    class Local {
      void m() {
        if (a) {}
      }
    }
    record LocalRecord(int x) {
      LocalRecord {
        if (x > 0) {}
      }
    }
  }

  boolean likeOperators(boolean a, boolean b, boolean c) {
    return a && b && c;
  }

  boolean likeOrOperators(boolean a, boolean b, boolean c) {
    return a || b || c;
  }

  boolean andThenOr(boolean a, boolean b, boolean c) {
    return a && b || c;
  }

  boolean orThenAnd(boolean a, boolean b, boolean c) {
    return a || b && c;
  }

  boolean mixedSequence(boolean a, boolean b, boolean c, boolean d, boolean e, boolean f) {
    return a && b && c || d || e && f;
  }

  boolean parenthesesDoNotBreakASequence(boolean a, boolean b, boolean c) {
    return a && (b && c);
  }

  boolean negationStartsANewSequence(boolean a, boolean b, boolean c) {
    return a && !(b && c);
  }

  boolean bitwiseOperatorsDoNotCount(boolean a, boolean b) {
    return a & b | a ^ b;
  }

  boolean sequenceInACallArgumentIsItsOwn(boolean a, boolean b, boolean c) {
    return a && check(b && c);
  }

  void labeledBreakAndContinue(int[][] grid) {
    outer:
    for (int[] row : grid) {
      for (int cell : row) {
        if (cell < 0) {
          continue outer;
        }
        if (cell > 9) {
          break outer;
        }
        if (cell == 5) {
          break;
        }
        if (cell == 6) {
          continue;
        }
      }
    }
  }

  int recursionCountsOnce(int n) {
    if (n <= 1) {
      return 1;
    }
    return recursionCountsOnce(n - 1) + this.recursionCountsOnce(n - 2);
  }

  void callOnOtherObjectIsNoRecursion(Rules other) {
    other.callOnOtherObjectIsNoRecursion(this);
  }

  void recursionInLambda(List<String> items) {
    items.forEach(item -> recursionInLambda(List.of()));
  }

  void synchronizedDoesNotNest(boolean a) {
    synchronized (this) {
      if (a) {}
    }
  }

  void run() {}

  boolean check(boolean a) {
    return a;
  }
}
