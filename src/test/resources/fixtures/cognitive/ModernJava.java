package fixtures.cognitive;

sealed interface Figure permits Circle, Square {}

record Circle(double radius) implements Figure {}

record Square(double side) implements Figure {}

class ModernJava {

  String switchExpression(int day) {
    String name = switch (day) {
      case 1, 7 -> "weekend";
      case 2 -> {
        if (day > 1) {
          yield "monday";
        }
        yield "unknown";
      }
      default -> "weekday";
    };
    return name;
  }

  double patternInstanceof(Object shape) {
    if (shape instanceof Circle circle && circle.radius() > 0) {
      return circle.radius();
    } else if (shape instanceof Square(double side)) {
      return side * side;
    }
    return 0;
  }

  String patternSwitch(Figure figure) {
    return switch (figure) {
      case Circle c when c.radius() > 10 -> "big circle";
      case Circle c -> "circle";
      case Square(double side) -> "square " + side;
    };
  }

  String textBlock() {
    String text = """
        if (x) { return y; }
        else { return z; }
        """;
    return text;
  }

  void unnamedVariables(Object value) {
    try {
      value.hashCode();
    } catch (RuntimeException _) {
      if (value instanceof Circle(_)) {
        value.notify();
      }
    }
  }
}
