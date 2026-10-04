package id.elclark.lunas.util

object MathParser {

    data class ParseResult(
        val terms: List<Long>,
        val total: Long,
        val isValid: Boolean,
        val formattedExpression: String
    )

    /**
     * Parses a string such as "12182 + 4252 + 1000 + 98744" or "12.182 + 4.252".
     * Ignores currency prefixes like "Rp", trims spaces, handles "+" and "-".
     */
    fun parse(input: String): ParseResult {
        if (input.isBlank()) {
            return ParseResult(emptyList(), 0L, false, "")
        }

        // Clean input: remove "rp", "idr", commas, dots (if used as thousand separator)
        var cleaned = input.lowercase()
            .replace("rp", "")
            .replace("idr", "")
            .trim()

        // Replace common math signs
        cleaned = cleaned.replace("×", "*").replace("x", "*")

        // Split by operators while preserving them
        // We match signed terms or split on '+'
        val terms = mutableListOf<Long>()
        val tokens = cleaned.split("+")

        var runningSum = 0L
        for (rawToken in tokens) {
            val token = rawToken.trim()
            if (token.isEmpty()) continue

            // If token contains multiplication (e.g. 3 * 115688)
            if (token.contains("*")) {
                val multParts = token.split("*")
                if (multParts.size == 2) {
                    val a = parseSingleNumber(multParts[0]) ?: return ParseResult(emptyList(), 0L, false, input)
                    val b = parseSingleNumber(multParts[1]) ?: return ParseResult(emptyList(), 0L, false, input)
                    val product = a * b
                    terms.add(product)
                    runningSum += product
                    continue
                }
            }

            // Normal single number
            val num = parseSingleNumber(token)
            if (num != null) {
                terms.add(num)
                runningSum += num
            } else {
                return ParseResult(emptyList(), 0L, false, input)
            }
        }

        if (terms.isEmpty()) {
            return ParseResult(emptyList(), 0L, false, input)
        }

        val formatted = terms.joinToString(" + ") { DateUtils.formatRupiah(it, withPrefix = false) }

        return ParseResult(
            terms = terms,
            total = runningSum,
            isValid = true,
            formattedExpression = formatted
        )
    }

    private fun parseSingleNumber(token: String): Long? {
        val sanitized = token.replace(".", "").replace(",", "").trim()
        return sanitized.toLongOrNull()
    }
}
