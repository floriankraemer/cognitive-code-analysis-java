// @cca-ignore in a file header does not ignore the first class
package fixtures.kotlin

/**
 * The whole class is ignored. @cca-ignore
 */
class IgnoredClass {
    fun hidden() {
    }
}

// @cca-ignore
fun ignoredTopLevel() {
}

fun byTrailingComment() {
} // @cca-ignore trails the function it follows and leaves the next one alone
fun keptAfterATrailingComment() {
}

// @cca-ignore separated by a blank line

fun bySeparatedComment() {
}

class PartiallyIgnored {

    /** @cca-ignore */
    fun byKDoc() {
    }

    /* @cca-ignore */
    @Deprecated("old")
    fun byBlockCommentBeforeAnnotations() {
    }

    @Deprecated("old")
    // @cca-ignore
    fun byCommentBetweenAnnotationAndName() {
    }

    fun kept() {
        // @cca-ignore inside a body ignores nothing
    }

    // @cca-ignore
    class IgnoredNested {
        fun hidden() {
        }
    }

    // @cca-ignore
    companion object {
        fun hidden() {
        }
    }

    class KeptNested {
        fun kept() {
        }
    }
}
