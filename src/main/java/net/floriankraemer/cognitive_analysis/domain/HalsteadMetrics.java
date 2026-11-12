package net.floriankraemer.cognitive_analysis.domain;

/**
 * The Halstead metrics of one method, derived from the operator and operand counts. Values are
 * unrounded.
 *
 * @param distinctOperators n1
 * @param distinctOperands n2
 * @param totalOperators N1
 * @param totalOperands N2
 */
public record HalsteadMetrics(
    int distinctOperators, int distinctOperands, int totalOperators, int totalOperands) {

  public int length() {
    return totalOperators + totalOperands;
  }

  public int vocabulary() {
    return distinctOperators + distinctOperands;
  }

  /** length * log2(vocabulary), 0 for an empty vocabulary. */
  public double volume() {
    return vocabulary() <= 0 ? 0.0 : length() * log2(vocabulary());
  }

  /** Exact for powers of two, so 8 gives 3.0 and not 2.9999999999999996. */
  private static double log2(final int value) {
    return Integer.bitCount(value) == 1 ? Integer.numberOfTrailingZeros(value) : Math.log(value) / Math.log(2);
  }

  /** (n1 / 2) * (N2 / n2), 0 without operands. */
  public double difficulty() {
    return distinctOperands == 0 ? 0.0 : distinctOperators / 2.0 * totalOperands / distinctOperands;
  }

  public double effort() {
    return difficulty() * volume();
  }
}
