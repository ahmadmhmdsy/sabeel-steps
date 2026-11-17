package com.sabeel.steps.data.export

import com.sabeel.steps.domain.DailyStep
import java.time.LocalDate
import java.util.Locale

/**
 * Pure RFC-4180-inspired CSV codec for the daily-steps file. No Android imports at all, so the
 * escaping/parsing rules are unit-tested on the JVM.
 *
 * Format (fixed header):
 *   date,steps,distance_meters,calories,active_minutes
 *   2026-11-16,8000,5632,320,54
 */
object CsvCodec {

    const val HEADER = "date,steps,distance_meters,calories,active_minutes"

    val HEADER_FIELDS: List<String> = listOf(
        "date",
        "steps",
        "distance_meters",
        "calories",
        "active_minutes",
    )

    /** Thrown when a file is not a Sabeel daily-steps export (or has no usable header). */
    class CsvFormatException(message: String) : IllegalArgumentException(message)

    /** Quotes a field only when needed and doubles embedded quotes (RFC 4180). */
    fun escape(field: String): String {
        val needsQuotes = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return field
        return '"' + field.replace("\"", "\"\"") + '"'
    }

    /** Joins already-raw values into one CSV record (each value is escaped). */
    fun encode(fields: List<String>): String = fields.joinToString(",") { escape(it) }

    /** Parses a whole CSV document into records; quoted commas, quotes and newlines are honoured. */
    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var fields = ArrayList<String>()
        val current = StringBuilder()
        var inQuotes = false
        var fieldPending = false
        var index = 0

        fun endField() {
            fields.add(current.toString())
            current.setLength(0)
            fieldPending = false
        }

        fun endRow() {
            endField()
            rows.add(fields)
            fields = ArrayList()
        }

        while (index < text.length) {
            val char = text[index]
            if (inQuotes) {
                if (char == '"') {
                    val next = if (index + 1 < text.length) text[index + 1] else null
                    if (next == '"') {
                        current.append('"')
                        index++
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(char)
                }
            } else {
                when (char) {
                    '"' -> {
                        inQuotes = true
                        fieldPending = true
                    }
                    ',' -> endField()
                    '\r' -> {
                        if (index + 1 < text.length && text[index + 1] == '\n') index++
                        endRow()
                    }
                    '\n' -> endRow()
                    else -> {
                        current.append(char)
                        fieldPending = true
                    }
                }
            }
            index++
        }
        if (fieldPending || current.isNotEmpty() || fields.isNotEmpty()) endRow()
        return rows
    }

    /** True when the first record is our header (case-insensitive, trimmed). */
    fun validateHeader(fields: List<String>): Boolean {
        if (fields.size < HEADER_FIELDS.size) return false
        return HEADER_FIELDS.indices.all { position ->
            fields[position].trim().equals(HEADER_FIELDS[position], ignoreCase = true)
        }
    }

    /** Locale-independent decimal text; trailing zeros are trimmed (10.50 -> "10.5", 10.00 -> "10"). */
    fun formatDecimal(value: Double, decimals: Int): String {
        if (!value.isFinite()) return "0"
        val format = "%." + decimals.coerceIn(0, 6) + "f"
        val formatted = String.format(Locale.US, format, value.coerceAtLeast(0.0))
        return if (formatted.contains('.')) formatted.trimEnd('0').trimEnd('.') else formatted
    }

    fun row(day: DailyStep): String = encode(
        listOf(
            day.date,
            day.steps.toString(),
            formatDecimal(day.distanceMeters, 2),
            formatDecimal(day.calories, 1),
            day.activeMinutes.toString(),
        ),
    )

    /** Serialises the whole history, header first, one day per line. */
    fun toCsv(days: List<DailyStep>): String = buildString {
        append(HEADER).append('\n')
        days.forEach { day -> append(row(day)).append('\n') }
    }

    data class DecodeResult(val rows: List<DailyStep>, val skipped: Int)

    /**
     * Validates the header and decodes every data line, skipping malformed ones.
     * @throws CsvFormatException when the document is empty or the header is not ours.
     */
    fun decode(text: String, updatedAt: Long = 0L): DecodeResult {
        val records = parse(text).filter { record -> record.any { it.isNotBlank() } }
        if (records.isEmpty()) throw CsvFormatException("CSV document is empty")
        if (!validateHeader(records.first())) throw CsvFormatException("Unexpected CSV header")
        val rows = ArrayList<DailyStep>(records.size - 1)
        var skipped = 0
        for (record in records.drop(1)) {
            val day = decodeRow(record, updatedAt)
            if (day == null) skipped++ else rows.add(day)
        }
        return DecodeResult(rows, skipped)
    }

    private fun decodeRow(fields: List<String>, updatedAt: Long): DailyStep? {
        if (fields.size < HEADER_FIELDS.size) return null
        return try {
            val date = LocalDate.parse(fields[0].trim()).toString()
            val steps = fields[1].trim().toInt()
            val distanceMeters = fields[2].trim().toDouble()
            val calories = fields[3].trim().toDouble()
            val activeMinutes = fields[4].trim().toInt()
            if (steps < 0 || distanceMeters < 0 || calories < 0 || activeMinutes < 0) {
                null
            } else {
                DailyStep(
                    date = date,
                    steps = steps,
                    distanceMeters = distanceMeters,
                    calories = calories,
                    activeMinutes = activeMinutes,
                    updatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis(),
                )
            }
        } catch (t: Throwable) {
            null
        }
    }
}
