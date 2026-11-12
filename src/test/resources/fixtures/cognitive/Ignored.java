package fixtures.cognitive;

/** @cca-ignore */
class IgnoredByJavadoc {
  void hidden() {
  }

  class NestedOfIgnored {
    void alsoHidden() {
    }
  }
}

// @cca-ignore: generated code
class IgnoredByLineComment {
  void hidden() {
  }
}

class PartiallyIgnored {

  /**
   * Does something.
   *
   * @cca-ignore
   */
  void ignoredByJavadoc() {
  }

  // @cca-ignore
  void ignoredByLineComment() {
  }

  // @cca-ignore because of reasons
  /** Javadoc closer to the method. */
  void ignoredByStackedComment() {
  }

  @Deprecated
  /* @cca-ignore */
  void ignoredBetweenAnnotationAndDeclaration() {
  }

  void ignoredByTrailingComment() {} // @cca-ignore

  void kept() {
    // @cca-ignore inside the body does not ignore the method
  }

  /* block comment @cca-ignore */
  PartiallyIgnored() {
  }

  /** @cca-ignore */
  static class IgnoredNested {
    void hidden() {
    }
  }

  // a comment without the tag
  class KeptNested {
    void kept() {
    }
  }
}
