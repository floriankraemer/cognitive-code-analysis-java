package shapes;

public class Shapes {

  private final int scale;

  public Shapes(int scale) {
    this.scale = scale;
  }

  public int area(int side) {
    return side * side * scale;
  }

  public int area(int width, int height) {
    if (width < 0) {
      return -1;
    }
    if (height < 0) {
      return -1;
    }
    return width * height * scale;
  }

  public String describe(int side) {
    if (side > 10) {
      return "large";
    }
    if (side > 5) {
      return "medium";
    }
    return "small";
  }

  public int unused(int value) {
    int doubled = value * 2;
    return doubled + scale;
  }

  public static class Inner {

    public int perimeter(int side) {
      return side * 4;
    }

    public int never(int side) {
      return side * 3;
    }
  }
}
