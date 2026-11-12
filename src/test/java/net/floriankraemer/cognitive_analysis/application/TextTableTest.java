package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextTableTest {

  private final TextTable table = new TextTable(1);

  @Test
  void putsSingleLineRowsDirectlyUnderEachOtherBelowTheHeaderLine() {
    assertThat(table.render(List.of("Name", "N"), List.of(List.of("a", "1"), List.of("b", "2")))).isEqualTo("""
        ┌──────┬───┐
        │ Name │ N │
        ├──────┼───┤
        │ a    │ 1 │
        │ b    │ 2 │
        └──────┴───┘""");
  }

  @Test
  void drawsALineBetweenTheRowsWhenABodyCellHasSeveralLines() {
    assertThat(table.render(List.of("Name", "N"), List.of(List.of("a", "1\nΔ +1"), List.of("b", "2")))).isEqualTo("""
        ┌──────┬──────┐
        │ Name │  N   │
        ├──────┼──────┤
        │ a    │  1   │
        │      │ Δ +1 │
        ├──────┼──────┤
        │ b    │  2   │
        └──────┴──────┘""");
  }

  @Test
  void ignoresMultiLineHeadersWhenDecidingOnRowLines() {
    assertThat(table.render(List.of("Na\nme", "N"), List.of(List.of("a", "1"), List.of("b", "2")))).isEqualTo("""
        ┌────┬───┐
        │ Na │ N │
        │ me │   │
        ├────┼───┤
        │ a  │ 1 │
        │ b  │ 2 │
        └────┴───┘""");
  }

  @Test
  void measuresTheWidthWithoutColorCodes() {
    assertThat(table.render(List.of("N"), List.of(List.of("\u001B[31m1\u001B[39m")))).isEqualTo("""
        ┌───┐
        │ N │
        ├───┤
        │ \u001B[31m1\u001B[39m │
        └───┘""");
  }

  @Test
  void countsWideCharactersAsTwoColumns() {
    assertThat(table.render(List.of("Name", "N"), List.of(List.of("メソッド", "1"), List.of("日本", "2")))).isEqualTo("""
        ┌──────────┬───┐
        │ Name     │ N │
        ├──────────┼───┤
        │ メソッド │ 1 │
        │ 日本     │ 2 │
        └──────────┴───┘""");
  }

  @Test
  void countsHangulFullwidthFormsAndEmojiAsTwoColumns() {
    assertThat(table.render(List.of("N"), List.of(List.of("한글"), List.of("Ａ"), List.of("🙂"), List.of("abcd"))))
        .isEqualTo("""
        ┌──────┐
        │ N    │
        ├──────┤
        │ 한글 │
        │ Ａ   │
        │ 🙂   │
        │ abcd │
        └──────┘""");
  }

  @Test
  void countsCombiningMarksJoinersAndVariationSelectorsAsZeroColumns() {
    // "e" + combining acute, and a heart with a variation selector, which is one column wide here
    assertThat(table.render(List.of("N"), List.of(List.of("e\u0301"), List.of("ab"), List.of("\u2764\uFE0F"))))
        .isEqualTo("""
        ┌────┐
        │ N  │
        ├────┤
        │ e\u0301  │
        │ ab │
        │ \u2764\uFE0F  │
        └────┘""");
  }

  @Test
  void countsAZeroWidthJoinerAsZeroColumns() {
    assertThat(table.render(List.of("N"), List.of(List.of("a\u200Db")))).isEqualTo("""
        ┌────┐
        │ N  │
        ├────┤
        │ a\u200Db │
        └────┘""");
  }
}
