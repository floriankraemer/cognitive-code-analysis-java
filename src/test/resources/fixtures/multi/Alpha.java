package multi;

class Alpha {

  void second() {
    System.out.println("2");
  }

  void first() {
    System.out.println("1");
  }
}

class Helper {

  void run(int a, int b) {
    if (a > b) {
      System.out.println("a");
    }
  }
}
