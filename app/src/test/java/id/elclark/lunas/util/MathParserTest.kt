package id.elclark.lunas.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathParserTest {

    @Test
    fun parseEmptyOrBlankReturnsInvalid() {
        val emptyResult = MathParser.parse("")
        assertFalse(emptyResult.isValid)
        assertEquals(0L, emptyResult.total)

        val blankResult = MathParser.parse("   ")
        assertFalse(blankResult.isValid)
        assertEquals(0L, blankResult.total)
    }

    @Test
    fun parseSingleNumberWithCurrency() {
        val res1 = MathParser.parse("50000")
        assertTrue(res1.isValid)
        assertEquals(50000L, res1.total)
        assertEquals(listOf(50000L), res1.terms)

        val res2 = MathParser.parse("Rp 50.000")
        assertTrue(res2.isValid)
        assertEquals(50000L, res2.total)
        assertEquals(listOf(50000L), res2.terms)

        val res3 = MathParser.parse("IDR 100,000")
        assertTrue(res3.isValid)
        assertEquals(100000L, res3.total)
    }

    @Test
    fun parseSimpleAddition() {
        val result = MathParser.parse("10000 + 20000 + 30000")
        assertTrue(result.isValid)
        assertEquals(60000L, result.total)
        assertEquals(listOf(10000L, 20000L, 30000L), result.terms)
        assertEquals("10.000 + 20.000 + 30.000", result.formattedExpression)
    }

    @Test
    fun parseSimpleSubtraction() {
        val result = MathParser.parse("50000 - 20000")
        assertTrue(result.isValid)
        assertEquals(30000L, result.total)
        assertEquals(listOf(50000L, -20000L), result.terms)
        assertEquals("50.000 - 20.000", result.formattedExpression)
    }

    @Test
    fun parseMixedAdditionAndSubtraction() {
        val result = MathParser.parse("Rp 100.000 + 50.000 - 25.000 - 5.000")
        assertTrue(result.isValid)
        assertEquals(120000L, result.total)
        assertEquals(listOf(100000L, 50000L, -25000L, -5000L), result.terms)
        assertEquals("100.000 + 50.000 - 25.000 - 5.000", result.formattedExpression)
    }

    @Test
    fun parseMultiplicationWithSubtraction() {
        val result = MathParser.parse("100000 - 2 * 15000")
        assertTrue(result.isValid)
        assertEquals(70000L, result.total)
        assertEquals(listOf(100000L, -30000L), result.terms)
        assertEquals("100.000 - 30.000", result.formattedExpression)
    }

    @Test
    fun parseMultiplicationWithCrossSymbols() {
        val res1 = MathParser.parse("3 x 20000 + 10000")
        assertTrue(res1.isValid)
        assertEquals(70000L, res1.total)

        val res2 = MathParser.parse("2 × 15000")
        assertTrue(res2.isValid)
        assertEquals(30000L, res2.total)
    }

    @Test
    fun parseLeadingMinus() {
        val result = MathParser.parse("-5000 + 15000")
        assertTrue(result.isValid)
        assertEquals(10000L, result.total)
        assertEquals(listOf(-5000L, 15000L), result.terms)
        assertEquals("- 5.000 + 15.000", result.formattedExpression)
    }

    @Test
    fun parseUnicodeMinuses() {
        val resEnDash = MathParser.parse("50000 – 20000")
        assertTrue(resEnDash.isValid)
        assertEquals(30000L, resEnDash.total)

        val resMinusSign = MathParser.parse("50000 \u2212 20000")
        assertTrue(resMinusSign.isValid)
        assertEquals(30000L, resMinusSign.total)
    }

    @Test
    fun parseInvalidExpressions() {
        assertFalse(MathParser.parse("abc").isValid)
        assertFalse(MathParser.parse("10000 +").isValid)
        assertFalse(MathParser.parse("10000 -").isValid)
        assertFalse(MathParser.parse("10000 ++ 5000").isValid)
        assertFalse(MathParser.parse("10000 +- 5000").isValid)
        assertFalse(MathParser.parse("10000 20000").isValid)
        assertFalse(MathParser.parse("3 * ").isValid)
        assertFalse(MathParser.parse("* 3").isValid)
        assertFalse(MathParser.parse("10000 * * 2").isValid)
    }

    @Test
    fun parseSubtractionWithoutSpaces() {
        val result = MathParser.parse("50000-20000")
        assertTrue(result.isValid)
        assertEquals(30000L, result.total)
        assertEquals(listOf(50000L, -20000L), result.terms)
        assertEquals("50.000 - 20.000", result.formattedExpression)
    }

    @Test
    fun parseArithmeticOverflowReturnsInvalid() {
        // Multiplication overflow
        val overflowMult = MathParser.parse("9999999999 * 9999999999")
        assertFalse(overflowMult.isValid)

        // Addition overflow
        val overflowAdd = MathParser.parse("9223372036854775000 + 1000000")
        assertFalse(overflowAdd.isValid)
    }
}
