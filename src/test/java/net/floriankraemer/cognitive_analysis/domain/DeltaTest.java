package net.floriankraemer.cognitive_analysis.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DeltaTest {

  @Test
  public void testGetValue() {
    Delta delta = new Delta(1, 2);
    assert delta.getValue() == 1;
  }

  @Test
  public void testHasChanged() {
    Delta delta = new Delta(1, 2);
    assertTrue(delta.hasChanged());

    Delta delta2 = new Delta(1, 1);
    assertFalse(delta2.hasChanged());
  }

  @Test
  public void testHasDecreased() {
    Delta delta = new Delta(2, 1);
    assertTrue(delta.hasDecreased());

    Delta delta2 = new Delta(1, 2);
    assertFalse(delta2.hasDecreased());

    assertFalse(new Delta(1, 1).hasDecreased());
  }

  @Test
  public void testHasIncreased() {
    Delta delta = new Delta(1, 2);
    assertTrue(delta.hasIncreased());
    assertEquals(1.0, delta.getValue());

    Delta delta2 = new Delta(1, 1);
    assertFalse(delta2.hasIncreased());
    assertEquals(0, delta2.getValue());
  }

  @Test
  public void testRoundedComparesAtThreeDecimalsHalfUp() {
    assertFalse(Delta.rounded(0.333, 1.0 / 3.0).hasChanged());
    assertEquals(0.001, Delta.rounded(0.3335, 0.3345).getValue(), 1e-12);
    assertTrue(Delta.rounded(1.0, 1.0006).hasIncreased());
    assertEquals(-1.5, Delta.rounded(3.0, 1.5).getValue());
  }
}
