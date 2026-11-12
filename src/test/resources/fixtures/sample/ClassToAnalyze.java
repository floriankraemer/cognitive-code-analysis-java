package test.testcode;

class ClassToAnalyze {

  public void simpleMethod() {
    System.out.println("Hello World!");
  }

  public void methodWithTwoArguments(String one, String two) {
    if (one.equals(two)) {
      System.out.println("Hello World!");
    } else {
      if (one.equals("foo")) {
        System.out.println("Hello World!");
      } else if (one.equals("bar")) {
        System.out.println("Hello World!");
        if (two.equals("baz")) {
          System.out.println("Hello World!");
        } else {
          System.out.println("Hello World!");
        }
      }
    }
  }
}
