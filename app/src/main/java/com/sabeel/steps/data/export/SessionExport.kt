package com.sabeel.steps.data.export

import com.sabeel.steps.domain.Walk
import com.sabeel.steps.domain.session.PlaceMath
import com.sabeel.steps.domain.session.WalkWithLegs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Pure builders for the opt-in sessions export (plan Phase 3). No Android imports, no I/O and no
 * network: the UI selects a Storage Access Framework URI and only writes the strings returned here.
 *
 * Both builders are total functions. Bad or partial input (missing route, invalid GPS fix, a
 * non-finite value) yields a valid skeleton or a skipped point; an exception is never thrown into
 * the UI. Decimal separators are always a dot (Locale.US) so files open in any spreadsheet locale,
 * and CSV fields are escaped with the same RFC 4180 rules as the daily export (see [CsvCodec]).
 */
object SessionExport {

    const val CSV_HEADER: String =
        "id,date,startedAt,endedAt,activity,status,steps,distanceMeters,durationSec," +
            "secondsPerKm,goalType,goalValue,targetMet,gpsUsed,startPlaceId,endPlaceId,presetId,legsCount"

    val CSV_HEADER_FIELDS: List<String> = CSV_HEADER.split(",")

    const val GPX_NAMESPACE = "http://www.topografix.com/GPX/1/1"
    const val NO_ROUTE_DESC = "No GPS route was recorded for this session."

    private const val CREATOR = "Sabeel"
    private const val COORDINATE_FORMAT = "%.6f"

    /**
     * Sessions CSV, header first, one row per saved session, oldest start first. The [zoneId] is
     * only used to turn [Walk.startedAt] into the human-readable date column; the numeric columns
     * stay locale-independent. Never throws.
     */
    fun buildSessionCsv(walks: List<WalkWithLegs>, zoneId: ZoneId = ZoneId.systemDefault()): String =
        runCatching {
            buildString {
                append(CSV_HEADER).append('\n')
                walks.sortedWith(compareBy<WalkWithLegs> { it.walk.startedAt }.thenBy { it.walk.id })
                    .forEach { session -> append(csvRow(session, zoneId)).append('\n') }
            }
        }.getOrElse { CSV_HEADER + "\n" }

    /**
     * One-GPX-track export of a saved session. Route points are written in recorded order; invalid
     * coordinates are skipped. When the session has no recorded route the GPX is still valid and
     * says so in <desc> (listing the midnight legs when the session crossed days). Never throws.
     */
    fun buildSessionGpx(walk: WalkWithLegs, name: String): String =
        runCatching { gpxText(walk, name) }.getOrElse { skeletonGpx(name) }

    /**
     * RFC 4180 field escaping used by the sessions file; exposed so the quoting rules can be tested
     * at the export boundary (it delegates to the same [CsvCodec.escape] the daily export uses).
     */
    fun escapeField(value: String): String = CsvCodec.escape(value)

    private fun csvRow(session: WalkWithLegs, zoneId: ZoneId): String {
        val walk = session.walk
        return CsvCodec.encode(
            listOf(
                walk.id.toString(),
                dateOf(walk, zoneId),
                walk.startedAt.toString(),
                walk.endedAt.toString(),
                walk.activityType.name,
                walk.status.name,
                walk.steps.toString(),
                CsvCodec.formatDecimal(walk.distanceMeters, 2),
                walk.durationSec.toString(),
                walk.secondsPerKm.toString(),
                walk.goalType.name,
                CsvCodec.formatDecimal(walk.goalValue, 2),
                walk.targetMet.toString(),
                walk.gpsUsed.toString(),
                walk.startPlaceId?.toString() ?: "",
                walk.endPlaceId?.toString() ?: "",
                walk.sessionPresetId?.toString() ?: "",
                session.legs.size.toString(),
            ),
        )
    }

    private fun dateOf(walk: Walk, zoneId: ZoneId): String = runCatching {
        Instant.ofEpochMilli(walk.startedAt).atZone(zoneId).toLocalDate().toString()
    }.getOrDefault("")

    private fun gpxText(session: WalkWithLegs, name: String): String {
        val walk = session.walk
        val safeName = name.ifBlank { defaultName(walk) }
        val points = walk.route.filter { PlaceMath.isValidCoordinate(it.lat, it.lng) }
        val description = if (points.isEmpty()) noRouteDescription(session) else ""
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append('\n')
            append("<gpx version=\"1.1\" creator=\"").append(xmlEscape(CREATOR)).append('"')
            append(" xmlns=\"").append(GPX_NAMESPACE).append('"')
            append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
            append(" xsi:schemaLocation=\"").append(GPX_NAMESPACE).append(' ')
            append(GPX_NAMESPACE).append("/gpx.xsd\">").append('\n')
            append("  <metadata>\n")
            append("    <name>").append(xmlEscape(safeName)).append("</name>\n")
            if (walk.startedAt > 0L) {
                append("    <time>").append(isoTime(walk.startedAt)).append("</time>\n")
            }
            append("  </metadata>\n")
            append("  <trk>\n")
            append("    <name>").append(xmlEscape(safeName)).append("</name>\n")
            if (description.isNotEmpty()) {
                append("    <desc>").append(xmlEscape(description)).append("</desc>\n")
            }
            append("    <trkseg>\n")
            points.forEach { point ->
                append("      <trkpt lat=\"").append(coordinate(point.lat))
                append("\" lon=\"").append(coordinate(point.lng)).append('"')
                if (point.t > 0L) {
                    append("><time>").append(isoTime(point.t)).append("</time></trkpt>").append('\n')
                } else {
                    append(" />").append('\n')
                }
            }
            append("    </trkseg>\n")
            append("  </trk>\n")
            append("</gpx>\n")
        }
    }

    private fun noRouteDescription(session: WalkWithLegs): String {
        if (session.legs.isEmpty()) return NO_ROUTE_DESC
        val legs = session.legs.joinToString(separator = ", ") { leg -> leg.date + " (" + leg.steps + " steps)" }
        return NO_ROUTE_DESC + " Days: " + legs
    }

    private fun defaultName(walk: Walk): String =
        if (walk.id > 0L) "Sabeel session " + walk.id else "Sabeel session"

    private fun skeletonGpx(name: String): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append('\n')
        append("<gpx version=\"1.1\" creator=\"").append(xmlEscape(CREATOR)).append('"')
        append(" xmlns=\"").append(GPX_NAMESPACE).append("\">").append('\n')
        append("  <trk>\n")
        append("    <name>").append(xmlEscape(name.ifBlank { CREATOR })).append("</name>\n")
        append("    <desc>").append(xmlEscape(NO_ROUTE_DESC)).append("</desc>\n")
        append("    <trkseg />\n")
        append("  </trk>\n")
        append("</gpx>\n")
    }

    private fun coordinate(value: Double): String =
        String.format(Locale.US, COORDINATE_FORMAT, value.coerceIn(-180.0, 180.0))

    private fun isoTime(epochMillis: Long): String =
        DateTimeFormatter.ISO_INSTANT.format(
            Instant.ofEpochMilli(epochMillis).truncatedTo(ChronoUnit.SECONDS),
        )

    /** XML 1.0 escaping plus removal of characters XML cannot represent (never throws). */
    private fun xmlEscape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (isValidXmlChar(char)) append(char) else append('?')
            }
        }
    }

    private fun isValidXmlChar(char: Char): Boolean = when (char.code) {
        0x9, 0xA, 0xD -> true
        in 0x20..0xD7FF -> true
        in 0xE000..0xFFFD -> true
        // Surrogate halves are kept: valid pairs form one legal code point above the BMP.
        in 0xD800..0xDFFF -> true
        else -> false
    }
}
