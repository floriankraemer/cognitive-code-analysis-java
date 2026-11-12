package shop;

import java.util.List;

class Invoice {

  private final List<Integer> amounts;

  Invoice(List<Integer> amounts) {
    this.amounts = amounts;
  }

  int total(boolean discounted) {
    int sum = 0;
    for (int amount : amounts) {
      if (amount > 0) {
        sum += amount;
      } else if (discounted) {
        sum -= amount;
      }
    }
    return sum;
  }
}
