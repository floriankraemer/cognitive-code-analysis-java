package net.floriankraemer.cognitive_analysis.application;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders rows of (possibly multi-line) cells as a box-drawing table.
 * Column widths follow the widest line of each column, ANSI color codes not counted.
 */
public final class TextTable {

  private static final Pattern ANSI_CODE = Pattern.compile("\u001B\\[[0-9;]*m");

  /** Spaces between the border and the text of a cell, on each side. */
  private static final int PADDING = 1;

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

    out.append(border('┌', '┬', '┐', widths)).append('\n');
    for (int i = 0; i < all.size(); i++) {
      if (i > 0) {
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

  private static int visibleLength(final String text) {
    return ANSI_CODE.matcher(text).replaceAll("").length();
  }
}
