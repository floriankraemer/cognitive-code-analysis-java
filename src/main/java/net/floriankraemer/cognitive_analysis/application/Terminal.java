package net.floriankraemer.cognitive_analysis.application;

import java.io.Console;
import java.lang.reflect.Method;
import java.util.function.Predicate;

/** Tells whether the process runs on a terminal rather than with redirected streams. */
final class Terminal {

  private static final Predicate<Console> CONSOLE_IS_TERMINAL = consoleTerminalCheck();

  private Terminal() {}

  /** True when the process has a console that is a terminal. */
  static boolean isAttached() {
    return isInteractive(System.console(), CONSOLE_IS_TERMINAL);
  }

  /** Input or output needs a console, and the console has to be a terminal rather than a redirected stream. */
  static <C> boolean isInteractive(final C console, final Predicate<C> isTerminal) {
    return console != null && isTerminal.test(console);
  }

  /**
   * On JDK 22 and later {@code System.console()} is non-null even when the streams are redirected, and
   * {@code Console.isTerminal()} tells the two cases apart.
   *
   * <p>ponytail: the compile release is 21, so {@code isTerminal} is looked up by reflection. Call it directly and
   * drop the lookup once the release is 22 or later.
   */
  static Predicate<Console> consoleTerminalCheck() {
    final Method isTerminal;
    try {
      isTerminal = Console.class.getMethod("isTerminal");
    } catch (NoSuchMethodException e) {
      // JDK 21: System.console() is only non-null when stdin and stdout are both terminals.
      return console -> true;
    }
    return console -> {
      try {
        return (Boolean) isTerminal.invoke(console);
      } catch (ReflectiveOperationException e) {
        return false;
      }
    };
  }
}
