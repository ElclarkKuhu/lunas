package id.elclark.lunas.util

object MathParser {

    data class ParseResult(
        val terms: List<Long>,
        val total: Long,
        val isValid: Boolean,
        val formattedExpression: String
    )

    /**
     * Parses a string such as "12182 + 4252 + 1000 + 98744", "50000 - 20000", or "12.182 - 4.252".
     * Ignores currency prefixes like "Rp", trims spaces, handles "+", "-", and "*".
     */
    fun parse(input: String): ParseResult {
        if (input.isBlank()) {
            return ParseResult(emptyList(), 0L, false, "")
        }

        // Clean input: remove "rp", "idr", replace unicode minuses and multiply symbols
        val cleaned = input.lowercase()
            .replace("rp", "")
            .replace("idr", "")
            .replace("×", "*")
            .replace("x", "*")
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .trim()

        if (cleaned.isEmpty()) {
            return ParseResult(emptyList(), 0L, false, input)
        }

        val terms = mutableListOf<Long>()
        var runningSum = 0L
        val formattedBuilder = StringBuilder()

        var i = 0
        val n = cleaned.length
        var currentSign = 1L
        var isFirstTerm = true

        while (i < n) {
            // Skip whitespace
            while (i < n && cleaned[i].isWhitespace()) i++
            if (i >= n) break

            val c = cleaned[i]
            if (c == '+' || c == '-') {
                currentSign = if (c == '-') -1L else 1L
                i++
                while (i < n && cleaned[i].isWhitespace()) i++
                if (i >= n || cleaned[i] == '+' || cleaned[i] == '-') {
                    // Trailing or consecutive operators -> invalid
                    return ParseResult(emptyList(), 0L, false, input)
                }
            } else if (!isFirstTerm) {
                // Two operands without an operator between them -> invalid
                return ParseResult(emptyList(), 0L, false, input)
            }

            // Read characters of the term until the next '+' or '-' (or end)
            val termStart = i
            while (i < n && cleaned[i] != '+' && cleaned[i] != '-') {
                i++
            }
            val rawTerm = cleaned.substring(termStart, i).trim()
            if (rawTerm.isEmpty()) {
                return ParseResult(emptyList(), 0L, false, input)
            }

            // Evaluate term (may contain '*')
            val termValue: Long
            try {
                if (rawTerm.contains("*")) {
                    val multParts = rawTerm.split("*")
                    if (multParts.any { it.isBlank() }) {
                        return ParseResult(emptyList(), 0L, false, input)
                    }
                    var product = 1L
                    for (part in multParts) {
                        val num = parseSingleNumber(part) ?: return ParseResult(emptyList(), 0L, false, input)
                        product = Math.multiplyExact(product, num)
                    }
                    termValue = product
                } else {
                    termValue = parseSingleNumber(rawTerm) ?: return ParseResult(emptyList(), 0L, false, input)
                }

                val signedValue = Math.multiplyExact(currentSign, termValue)
                terms.add(signedValue)
                runningSum = Math.addExact(runningSum, signedValue)
            } catch (_: ArithmeticException) {
                return ParseResult(emptyList(), 0L, false, input)
            }

            if (isFirstTerm) {
                if (currentSign < 0) {
                    formattedBuilder.append("- ")
                }
                formattedBuilder.append(DateUtils.formatRupiah(termValue, withPrefix = false))
            } else {
                if (currentSign < 0) {
                    formattedBuilder.append(" - ")
                } else {
                    formattedBuilder.append(" + ")
                }
                formattedBuilder.append(DateUtils.formatRupiah(termValue, withPrefix = false))
            }

            isFirstTerm = false
        }

        if (terms.isEmpty()) {
            return ParseResult(emptyList(), 0L, false, input)
        }

        return ParseResult(
            terms = terms,
            total = runningSum,
            isValid = true,
            formattedExpression = formattedBuilder.toString()
        )
    }

    private fun parseSingleNumber(token: String): Long? {
        val sanitized = token.replace(".", "").replace(",", "").trim()
        return sanitized.toLongOrNull()
    }
}
