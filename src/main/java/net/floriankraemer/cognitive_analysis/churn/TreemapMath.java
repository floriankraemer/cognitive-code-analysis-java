package net.floriankraemer.cognitive_analysis.churn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The maths behind the SVG treemap: item selection, score normalisation, colours and the slice-and-dice layout. */
final class TreemapMath {

  private static final double COLOR_MIN = 0;
  private static final double COLOR_MAX = 10;

  record Item(String className, double churn, double score) {}

  record Rect(double x, double y, double width, double height, Item item) {}

  private TreemapMath() {}

  /** The classes with churn above zero, biggest churn first. */
  static List<Item> prepareItems(final List<ChurnMetrics> metrics) {
    return metrics.stream()
        .filter(metric -> metric.churn() > 0)
        .map(metric -> new Item(metric.className(), metric.churn(), metric.score()))
        .sorted(Comparator.comparingDouble(Item::churn).reversed())
        .toList();
  }

  /** The score range that is mapped to the colour scale; the fixed 0 to 10 scale when there is no spread. */
  static double[] findScoreRange(final List<Item> items) {
    final double min = items.stream().mapToDouble(Item::score).min().orElse(COLOR_MIN);
    final double max = items.stream().mapToDouble(Item::score).max().orElse(COLOR_MAX);
    if (items.isEmpty() || min == max) {
      return new double[] {COLOR_MIN, COLOR_MAX};
    }
    return new double[] {min, max};
  }

  /** Maps the score onto 0 to 10; 0 when the range is empty. */
  static double normalizeScore(final double score, final double min, final double max) {
    if (max > min) {
      return COLOR_MAX * (score - min) / (max - min);
    }
    return 0;
  }

  /** Green for 0 to red for 10. */
  static String scoreToColor(final double score) {
    final double clamped = Math.max(COLOR_MIN, Math.min(COLOR_MAX, score));
    final int red = (int) (255 * (clamped / COLOR_MAX));
    final int green = (int) (180 * (1 - clamped / COLOR_MAX));
    return "rgb(" + red + "," + green + ",80)";
  }

  /** Slice-and-dice layout, starting with a vertical cut, alternating the orientation per level. */
  static List<Rect> layout(final List<Item> items, final double width, final double height) {
    final List<Rect> rects = new ArrayList<>();
    layout(items, new Area(0, 0, width, height), true, rects);
    return rects;
  }

  private static void layout(
      final List<Item> items, final Area area, final boolean vertical, final List<Rect> rects) {
    if (items.isEmpty()) {
      return;
    }
    if (items.size() == 1) {
      rects.add(new Rect(area.x(), area.y(), area.width(), area.height(), items.get(0)));
      return;
    }

    final double sum = sum(items);
    final int split = findSplitIndex(items, sum);
    final List<Item> first = items.subList(0, split);
    final List<Area> parts = area.cut(sum(first) / sum, vertical);
    layout(first, parts.get(0), !vertical, rects);
    layout(items.subList(split, items.size()), parts.get(1), !vertical, rects);
  }

  private record Area(double x, double y, double width, double height) {

    /** Cuts the area in two, the first part taking the share of the width (vertical cut) or the height. */
    List<Area> cut(final double share, final boolean vertical) {
      if (vertical) {
        final double firstWidth = width * share;
        return List.of(new Area(x, y, firstWidth, height), new Area(x + firstWidth, y, width - firstWidth, height));
      }
      final double firstHeight = height * share;
      return List.of(new Area(x, y, width, firstHeight), new Area(x, y + firstHeight, width, height - firstHeight));
    }
  }

  /** The count of items up to and including the first one that brings the running sum to half of the total. */
  private static int findSplitIndex(final List<Item> items, final double sum) {
    double accumulated = 0;
    for (int i = 0; i < items.size(); i++) {
      accumulated += items.get(i).churn();
      if (accumulated >= sum / 2) {
        final int split = i + 1;
        return split >= items.size() ? 1 : split;
      }
    }
    return 1;
  }

  private static double sum(final List<Item> items) {
    return items.stream().mapToDouble(Item::churn).sum();
  }
}
