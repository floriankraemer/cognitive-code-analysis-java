package shop

class OrderService(private val invoices: List<Invoice>) {

    fun status(code: Int, express: Boolean): String = when (code) {
        0 -> "new"
        1, 2 -> if (express) "shipping fast" else "shipping"
        else -> "unknown"
    }

    fun totals(discounted: Boolean): List<Int> {
        val result = mutableListOf<Int>()
        for (invoice in invoices) {
            val total = invoice.total(discounted)
            if (total > LIMIT && discounted || total < 0) {
                result.add(total)
            }
        }
        return result
    }

    companion object {
        const val LIMIT = 100

        fun create(): OrderService = OrderService(emptyList())
    }
}

fun OrderService.firstStatus(code: Int?): String = status(code ?: 0, false)
