package fixtures.understandability;

class Recursion {

  static int className(int n) {
    return Recursion.className(n - 1);
  }

  int qualifiedThis(int n) {
    return Recursion.this.qualifiedThis(n - 1);
  }

  int countsOnce(int n) {
    countsOnce(n);
    this.countsOnce(n);
    return Recursion.countsOnce(n);
  }

  int overload(int n) {
    return overload("x");
  }

  int overload(String s) {
    return 0;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  static int otherClassName(int n) {
    return Other.otherClassName(n);
  }

  int otherObject(Recursion other) {
    return other.otherObject(this);
  }

  class Inner {

    int innerThis(int n) {
      return Inner.this.innerThis(n - 1);
    }

    int outerThis(int n) {
      return Recursion.this.outerThis(n - 1);
    }

    int outerClassName(int n) {
      return Recursion.outerClassName(n - 1);
    }
  }
}
