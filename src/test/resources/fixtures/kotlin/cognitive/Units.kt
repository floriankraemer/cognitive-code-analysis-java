package fixtures.kotlin

fun topLevel(value: Int): Int {
    return value
}

fun String.shout(): String = uppercase()

fun overloadedTopLevel(value: Int) = value

fun overloadedTopLevel(value: String?, vararg rest: List<String>) = value

fun Int.overloadedTopLevel(handler: (Int) -> Unit) = handler(this)

abstract class Units(val size: Int) {

    constructor() : this(0)

    constructor(text: String) : this(text.length) {
        if (text.isEmpty()) {
            println("empty")
        }
    }

    init {
        if (size < 0) throw IllegalArgumentException()
    }

    abstract fun abstractFunction(a: Int, b: Int)

    fun overloaded() {
    }

    fun overloaded(names: List<String>, index: Map<String, List<Int>>) {
    }

    fun overloaded(values: IntArray, raw: kotlin.collections.List<*>) {
    }

    fun Int.extensionMember(): Int = this + size

    val computed: Int
        get() {
            if (size > 0) return size
            return 0
        }

    class Nested {
        fun inNested() {
        }

        inner class Deeper {
            fun inDeeper() {
            }
        }
    }

    companion object {
        fun create(): Units? = null
    }
}

object Registry {
    fun register(name: String) {
        println(name)
    }
}

class NamedCompanion {
    companion object Factory {
        fun build() = NamedCompanion()
    }
}

interface Shape {
    fun area(): Double

    fun describe(): String {
        return "area " + area()
    }
}

enum class Color {
    RED {
        override fun code() = 1
    },
    GREEN;

    open fun code(): Int {
        return 0
    }
}

data class Point(val x: Int, val y: Int) {
    fun sum() = x + y
}
