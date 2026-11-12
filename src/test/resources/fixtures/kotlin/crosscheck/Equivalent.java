package fixtures.crosscheck;

import java.util.List;

class Equivalent {

  int count;

  int chain(int a) {
    if (a > 3) {
      return 3;
    } else if (a > 2) {
      return 2;
    } else {
      return 0;
    }
  }

  void nesting(List<Integer> values, boolean flag) {
    for (int value : values) {
      if (value > 0) {
        if (flag) {
          log(value);
        } else {
          log(0);
        }
      }
    }
  }

  void loops(List<Integer> values) {
    int i = 0;
    while (i < 10) {
      i++;
    }
    do {
      i--;
    } while (i > 0);
    for (int value : values) {
      log(value);
    }
  }

  void switchLikeWhen(int a) {
    switch (a) {
      case 1, 2 -> log(1);
      case 3 -> {
        if (a > 0) {
          log(3);
        }
      }
      default -> log(0);
    }
  }

  void tryCatch() {
    try {
      work();
    } catch (IllegalStateException e) {
      log(e);
    } catch (RuntimeException e) {
      log(e);
    }
  }

  void lambdas(List<Integer> values) {
    values.forEach(value -> {
      if (value > 0) {
        log(value);
      }
    });
  }

  boolean logical(boolean a, boolean b, boolean c) {
    return a && b || c && !a;
  }

  int properties(Equivalent other) {
    int sum = this.count + other.count;
    return sum + count;
  }

  int recursive(int n) {
    if (n <= 1) {
      return 1;
    }
    return n * recursive(n - 1);
  }

  void labeled(List<List<Integer>> rows) {
    outer:
    for (List<Integer> row : rows) {
      for (int cell : row) {
        if (cell < 0) {
          continue outer;
        }
      }
    }
  }

  void work() {
  }

  void log(Object value) {
  }
}
