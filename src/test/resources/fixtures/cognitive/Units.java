package fixtures.cognitive;

import java.util.List;
import java.util.Map;

abstract class Units {

  Units() {
  }

  Units(int size) {
    this();
  }

  abstract void abstractMethod(int a, int b);

  void overloaded() {
  }

  void overloaded(List<String> names, Map<String, List<Integer>> index) {
  }

  void overloaded(String... names) {
  }

  void overloaded(int[] values, java.util.List<?> raw) {
  }

  <T extends Comparable<T>> T generic(T value) {
    return value;
  }

  static class StaticNested {
    void inStatic() {
    }

    class Deeper {
      void inDeeper() {
      }
    }
  }

  class Inner {
    Inner() {
    }
  }

  enum Color {
    RED {
      @Override
      int code() {
        if (true) {
          return 1;
        }
        return 2;
      }
    },
    GREEN;

    int code() {
      return 0;
    }
  }

  record Point(int x, int y) {
    Point {
      if (x < 0) {
        throw new IllegalArgumentException();
      }
    }

    Point(int both) {
      this(both, both);
    }

    int sum() {
      return x + y;
    }
  }

  interface Shape {
    double area();

    default String describe() {
      return "area " + area();
    }

    static Shape unit() {
      return () -> 1.0;
    }

    private void helper() {
    }
  }

  @interface Marker {
    String value() default "";
  }
}
