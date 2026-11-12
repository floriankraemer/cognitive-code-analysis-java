package ledger

class Ledger(private val entries: List<Int>) {

    constructor(single: Int) : this(listOf(single)) {
        if (single < 0) {
            println("negative")
        }
    }

    fun balance(): Int {
        var sum = 0
        for (entry in entries) {
            sum += entry
        }
        return sum
    }

    fun unused(limit: Int): Int {
        if (limit > 0) {
            return limit
        }
        return 0
    }

    internal fun audit(): Boolean = entries.isNotEmpty()

    companion object {
        fun empty(): Ledger = Ledger(emptyList())

        fun neverCalled(): Ledger = Ledger(1)
    }
}

fun Ledger.describe(): String = if (balance() > 0) "positive" else "zero"

fun main() {
    val ledger = Ledger(3)
    println(ledger.balance())
    println(ledger.audit())
    println(Ledger.empty().describe())
}
