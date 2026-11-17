package com.sabeel.steps.data.export

import com.sabeel.steps.domain.DailyStep
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the CSV escaping/parsing rules used by export/import. */
class CsvCodecTest {

    @Test
    fun plainValuesNeedNoQuotes() {
        assertEquals("8000", CsvCodec.escape("8000"))
        assertEquals("2026-11-16", CsvCodec.escape("2026-11-16"))
    }

    @Test
    fun commasQuotesAndNewlinesAreQuoted() {
        assertEquals("\"a,b\"", CsvCodec.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvCodec.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvCodec.escape("line1\nline2"))
        assertEquals("\"a\r\"", CsvCodec.escape("a\r"))
    }

    @Test
    fun headerIsValidatedCaseInsensitively() {
        assertTrue(CsvCodec.validateHeader(CsvCodec.HEADER.split(",")))
        assertTrue(CsvCodec.validateHeader(listOf(" Date ", "STEPS", "distance_meters", "Calories", "active_minutes")))
        assertFalse(CsvCodec.validateHeader(listOf("date", "steps")))
        assertFalse(CsvCodec.validateHeader(listOf("name", "steps", "distance_meters", "calories", "active_minutes")))
    }

    @Test
    fun parseHandlesQuotedCommasAndDoubledQuotes() {
        val rows = CsvCodec.parse("a,\"b,c\",\"say \"\"hi\"\"\"\n1,2,3\n")
        assertEquals(2, rows.size)
        assertEquals(listOf("a", "b,c", "say \"hi\""), rows[0])
        assertEquals(listOf("1", "2", "3"), rows[1])
    }

    @Test
    fun parseHandlesCrLfAndMissingTrailingNewline() {
        val rows = CsvCodec.parse("a,b\r\nc,d")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), rows)
    }

    @Test
    fun parseKeepsEmbeddedNewlinesInsideQuotes() {
        val rows = CsvCodec.parse("\"line1\nline2\",x\n")
        assertEquals(listOf("line1\nline2", "x"), rows[0])
    }

    @Test
    fun roundTripPreservesEveryValue() {
        val days = listOf(
            DailyStep("2026-11-16", 8000, distanceMeters = 5632.0, calories = 320.0, activeMinutes = 54),
            DailyStep("2026-11-15", 12345, distanceMeters = 8796.42, calories = 493.8, activeMinutes = 61),
        )
        val decoded = CsvCodec.decode(CsvCodec.toCsv(days), updatedAt = 1700000000000L)
        assertEquals(0, decoded.skipped)
        assertEquals(2, decoded.rows.size)
        assertEquals(days[0], decoded.rows[0].copy(updatedAt = 0L))
        assertEquals(days[1], decoded.rows[1].copy(updatedAt = 0L))
        assertEquals(1700000000000L, decoded.rows[0].updatedAt)
    }

    @Test
    fun malformedLinesAreSkippedAndCounted() {
        val text = CsvCodec.HEADER + "\n" +
            "2026-11-16,8000,5632,320,54\n" +
            "not-a-date,1,2,3,4\n" +
            "2026-11-15,not-a-number,1,2,3\n" +
            "\n" +
            "2026-11-14,9000,6000,350,60\n"
        val decoded = CsvCodec.decode(text)
        assertEquals(2, decoded.rows.size)
        assertEquals(2, decoded.skipped)
        assertEquals("2026-11-16", decoded.rows[0].date)
        assertEquals("2026-11-14", decoded.rows[1].date)
    }

    @Test
    fun invalidHeaderThrows() {
        try {
            CsvCodec.decode("date,oops\n2026-11-16,1\n")
            throw AssertionError("expected CsvFormatException")
        } catch (expected: CsvCodec.CsvFormatException) {
            assertTrue(expected.message!!.isNotEmpty())
        }
    }

    @Test
    fun emptyDocumentThrows() {
        try {
            CsvCodec.decode("")
            throw AssertionError("expected CsvFormatException")
        } catch (expected: CsvCodec.CsvFormatException) {
            assertTrue(expected.message!!.isNotEmpty())
        }
    }

    @Test
    fun extraColumnsAreToleratedOnImport() {
        val text = CsvCodec.HEADER + ",extra\n2026-11-16,8000,5632,320,54,ignored\n"
        val decoded = CsvCodec.decode(text)
        assertEquals(1, decoded.rows.size)
        assertEquals(8000, decoded.rows[0].steps)
    }

    @Test
    fun decimalsAreLocaleIndependent() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("5632.5", CsvCodec.formatDecimal(5632.5, 2))
            assertEquals("320", CsvCodec.formatDecimal(320.0, 1))
            val csv = CsvCodec.toCsv(listOf(DailyStep("2026-11-16", 1, 1.25, 1.0, 1)))
            assertTrue(csv.contains("2026-11-16,1,1.25,1,1"))
        } finally {
            Locale.setDefault(original)
        }
    }
}