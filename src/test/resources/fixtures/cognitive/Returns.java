package fixtures.cognitive;

import java.util.List;
import java.util.function.Function;

class Returns {

  Function<Integer, Integer> returnsInLambdas() {
    Function<Integer, Integer> block = x -> {
      if (x > 0) {
        return x;
      }
      return -x;
    };
    Function<Integer, Integer> expression = x -> x + 1;
    return block.andThen(expression);
  }

  Object anonymousClassIsSkipped(int seed) {
    int capacity = seed;
    List<Integer> list = new java.util.ArrayList<>(capacity) {
      private int hidden = 1;

      @Override
      public boolean add(Integer value) {
        if (hidden > 0) {
          return super.add(value + this.hidden);
        }
        return false;
      }
    };
    return list;
  }

  int localClassIsSkipped(int seed) {
    class Counter {
      int count(int start) {
        if (start > 0) {
          return start;
        }
        return 0;
      }
    }
    record Pair(int left, int right) {
      Pair {
        if (left > right) {
          throw new IllegalArgumentException();
        }
      }
    }
    return new Counter().count(seed) + new Pair(1, 2).left();
  }
}
