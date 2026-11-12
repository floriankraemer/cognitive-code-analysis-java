package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import net.floriankraemer.cognitive_analysis.churn.TreemapMath.Item;
import net.floriankraemer.cognitive_analysis.churn.TreemapMath.Rect;
import org.junit.jupiter.api.Test;

class TreemapMathTest {

  private static ChurnMetrics metric(final String name, final double score, final double churn) {
    return new ChurnMetrics(name, name + ".java", score, 1, churn, null, null, null);
  }

  @Test
  void prepareItemsDropsClassesWithoutChurnAndSortsTheRestDescending() {
    final List<Item> items = TreemapMath.prepareItems(
        List.of(metric("small", 1, 2), metric("none", 1, 0), metric("big", 3, 9)));

    assertThat(items).containsExactly(new Item("big", 9, 3), new Item("small", 2, 1));
  }

  @Test
  void prepareItemsKeepsTheInputOrderOfEqualChurn() {
    final List<Item> items = TreemapMath.prepareItems(List.of(metric("b", 1, 2), metric("a", 1, 2)));

    assertThat(items).extracting(Item::className).containsExactly("b", "a");
  }

  @Test
  void findScoreRangeUsesTheMinimumAndMaximumScore() {
    assertThat(TreemapMath.findScoreRange(List.of(new Item("a", 1, 0.693), new Item("b", 1, 2.042))))
        .containsExactly(0.693, 2.042);
  }

  @Test
  void findScoreRangeFallsBackToTheFixedScaleWithoutSpread() {
    assertThat(TreemapMath.findScoreRange(List.of(new Item("a", 1, 3), new Item("b", 1, 3)))).containsExactly(0, 10);
    assertThat(TreemapMath.findScoreRange(List.of())).containsExactly(0, 10);
  }

  @Test
  void normalizeScoreMapsTheRangeOntoZeroToTen() {
    assertThat(TreemapMath.normalizeScore(2, 2, 6)).isEqualTo(0);
    assertThat(TreemapMath.normalizeScore(4, 2, 6)).isEqualTo(5);
    assertThat(TreemapMath.normalizeScore(6, 2, 6)).isEqualTo(10);
    assertThat(TreemapMath.normalizeScore(4, 4, 4)).isEqualTo(0);
    assertThat(TreemapMath.normalizeScore(4, 6, 2)).isEqualTo(0);
  }

  @Test
  void scoreToColorGoesFromGreenToRedAndClampsOutOfRangeScores() {
    assertThat(TreemapMath.scoreToColor(0)).isEqualTo("rgb(0,180,80)");
    assertThat(TreemapMath.scoreToColor(5)).isEqualTo("rgb(127,90,80)");
    assertThat(TreemapMath.scoreToColor(10)).isEqualTo("rgb(255,0,80)");
    assertThat(TreemapMath.scoreToColor(-3)).isEqualTo("rgb(0,180,80)");
    assertThat(TreemapMath.scoreToColor(13)).isEqualTo("rgb(255,0,80)");
    assertThat(TreemapMath.scoreToColor(1.2)).isEqualTo("rgb(30,158,80)");
  }

  @Test
  void layoutOfNothingIsEmpty() {
    assertThat(TreemapMath.layout(List.of(), 1200, 800)).isEmpty();
  }

  @Test
  void layoutOfOneItemFillsTheCanvas() {
    final Item item = new Item("a", 5, 1);

    assertThat(TreemapMath.layout(List.of(item), 1200, 800)).containsExactly(new Rect(0, 0, 1200, 800, item));
  }

  /** Same data as the PHP golden file: a vertical cut, then a horizontal cut of the left part. */
  @Test
  void layoutMatchesTheReferenceImplementation() {
    final List<Item> items = List.of(
        new Item("a", 12.252, 2.042), new Item("b", 8.06, 0.806), new Item("c", 5.544, 0.693));

    final List<Rect> rects = TreemapMath.layout(items, 1200, 800);

    assertThat(rects).hasSize(3);
    assertRect(rects.get(0), 0, 0, 942.70, 482.55);
    assertRect(rects.get(1), 0, 482.55, 942.70, 317.45);
    assertRect(rects.get(2), 942.70, 0, 257.30, 800);
    assertThat(rects).extracting(rect -> rect.item().className()).containsExactly("a", "b", "c");
  }

  @Test
  void layoutSplitsAtTheFirstItemThatReachesHalfOfTheTotal() {
    final List<Item> items = List.of(new Item("a", 5, 1), new Item("b", 3, 1), new Item("c", 2, 1));

    final List<Rect> rects = TreemapMath.layout(items, 100, 100);

    assertRect(rects.get(0), 0, 0, 50, 100);
    assertRect(rects.get(1), 50, 0, 50, 60);
    assertRect(rects.get(2), 50, 60, 50, 40);
  }

  @Test
  void layoutAlternatesTheOrientationPerLevel() {
    final List<Item> items = List.of(new Item("a", 4, 1), new Item("b", 3, 1), new Item("c", 2, 1), new Item("d", 1, 1));

    final List<Rect> rects = TreemapMath.layout(items, 100, 100);

    // vertical cut at 70 %, then each part is cut horizontally
    assertRect(rects.get(0), 0, 0, 70, 57.14);
    assertRect(rects.get(1), 0, 57.14, 70, 42.86);
    assertRect(rects.get(2), 70, 0, 30, 66.67);
    assertRect(rects.get(3), 70, 66.67, 30, 33.33);
  }

  @Test
  void layoutGivesTheLastItemTheRestWhenTheFirstOneAloneIsMoreThanHalf() {
    final List<Rect> rects = TreemapMath.layout(List.of(new Item("a", 9, 1), new Item("b", 1, 1)), 100, 100);

    assertRect(rects.get(0), 0, 0, 90, 100);
    assertRect(rects.get(1), 90, 0, 10, 100);
  }

  @Test
  void layoutOfTwoEqualItemsSplitsInTheMiddle() {
    final List<Rect> rects = TreemapMath.layout(List.of(new Item("a", 1, 1), new Item("b", 1, 1)), 100, 100);

    assertRect(rects.get(0), 0, 0, 50, 100);
    assertRect(rects.get(1), 50, 0, 50, 100);
  }

  private static void assertRect(final Rect rect, final double x, final double y, final double width, final double height) {
    assertThat(rect.x()).isCloseTo(x, within(0.01));
    assertThat(rect.y()).isCloseTo(y, within(0.01));
    assertThat(rect.width()).isCloseTo(width, within(0.01));
    assertThat(rect.height()).isCloseTo(height, within(0.01));
  }
}
