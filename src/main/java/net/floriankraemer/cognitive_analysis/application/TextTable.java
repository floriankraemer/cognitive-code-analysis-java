package net.floriankraemer.cognitive_analysis.application;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders rows of (possibly multi-line) cells as a box-drawing table.
 * Rows sit directly under each other unless a body cell spans several lines, which draws a line between
 * the rows. Column widths follow the widest line of each column, ANSI color codes not counted.
 */
public final class TextTable {

  private static final Pattern ANSI_CODE = Pattern.compile("\u001B\\[[0-9;]*m");

  /** Spaces between the border and the text of a cell, on each side. */
  private static final int PADDING = 1;

  private static final int ZERO_WIDTH_JOINER = 0x200D;

  /** Columns from this index on are centered; earlier ones are left-aligned. */
  private final int firstCenteredColumn;

  public TextTable(final int firstCenteredColumn) {
    this.firstCenteredColumn = firstCenteredColumn;
  }

  public String render(final List<String> header, final List<List<String>> rows) {
    final List<List<String>> all = new ArrayList<>();
    all.add(header);
    all.addAll(rows);

    final int[] widths = columnWidths(all, header.size());
    final StringBuilder out = new StringBuilder();
    final boolean separateRows = rows.stream().anyMatch(row -> row.stream().anyMatch(cell -> cell.contains("\n")));

    out.append(border('┌', '┬', '┐', widths)).append('\n');
    for (int i = 0; i < all.size(); i++) {
      if (i == 1 || (i > 1 && separateRows)) {
        out.append(border('├', '┼', '┤', widths)).append('\n');
      }
      appendRow(out, all.get(i), widths);
    }
    out.append(border('└', '┴', '┘', widths));

    return out.toString();
  }

  private int[] columnWidths(final List<List<String>> rows, final int columns) {
    final int[] widths = new int[columns];
    for (List<String> row : rows) {
      for (int c = 0; c < columns; c++) {
        for (String line : row.get(c).split("\n", -1)) {
          widths[c] = Math.max(widths[c], visibleLength(line));
        }
      }
    }
    return widths;
  }

  private void appendRow(final StringBuilder out, final List<String> row, final int[] widths) {
    final List<String[]> cells = row.stream().map(cell -> cell.split("\n", -1)).toList();
    final int height = cells.stream().mapToInt(lines -> lines.length).max().orElse(1);

    for (int line = 0; line < height; line++) {
      out.append('│');
      for (int c = 0; c < widths.length; c++) {
        final String text = line < cells.get(c).length ? cells.get(c)[line] : "";
        out.append(pad(text, widths[c], c >= firstCenteredColumn)).append('│');
      }
      out.append('\n');
    }
  }

  private String pad(final String text, final int width, final boolean center) {
    final int space = width - visibleLength(text);
    final int left = center ? space / 2 : 0;
    return " ".repeat(PADDING + left) + text + " ".repeat(PADDING + space - left);
  }

  private String border(final char left, final char middle, final char right, final int[] widths) {
    final StringBuilder line = new StringBuilder().append(left);
    for (int c = 0; c < widths.length; c++) {
      line.append("─".repeat(widths[c] + 2 * PADDING));
      line.append(c < widths.length - 1 ? middle : right);
    }
    return line.toString();
  }

  /**
   * The number of terminal columns of a text: color codes take none, combining marks take none, and wide
   * characters take two.
   *
   * <p>ponytail: the wide ranges are an approximate East Asian Width table (CJK, Hiragana, Katakana, Hangul,
   * fullwidth forms and the common emoji blocks). Characters outside it count as one column; take a full
   * Unicode table if exact widths of rare scripts matter.
   */
  private static int visibleLength(final String text) {
    final String plain = ANSI_CODE.matcher(text).replaceAll("");
    int width = 0;
    for (int i = 0; i < plain.length(); ) {
      final int codePoint = plain.codePointAt(i);
      width += columns(codePoint);
      i += Character.charCount(codePoint);
    }
    return width;
  }

  private static int columns(final int codePoint) {
    final int type = Character.getType(codePoint);
    if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
        || codePoint == ZERO_WIDTH_JOINER || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)) {
      return 0;
    }
    return isWide(codePoint) ? 2 : 1;
  }

  private static boolean isWide(final int c) {
    return (c >= 0x1100 && c <= 0x115F)
        || (c >= 0x2E80 && c <= 0xA4CF)
        || (c >= 0xAC00 && c <= 0xD7A3)
        || (c >= 0xF900 && c <= 0xFAFF)
        || (c >= 0xFE30 && c <= 0xFE6F)
        || (c >= 0xFF00 && c <= 0xFF60)
        || (c >= 0xFFE0 && c <= 0xFFE6)
        || (c >= 0x1F300 && c <= 0x1F64F)
        || (c >= 0x1F680 && c <= 0x1F6FF)
        || (c >= 0x1F900 && c <= 0x1F9FF)
        || (c >= 0x20000 && c <= 0x3FFFD);
  }
}
