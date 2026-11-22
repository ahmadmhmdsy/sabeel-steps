package com.sabeel.steps.data.export

import com.sabeel.steps.domain.RoutePoint
import com.sabeel.steps.domain.Walk
import com.sabeel.steps.domain.session.ActivityType
import com.sabeel.steps.domain.session.GoalType
import com.sabeel.steps.domain.session.SessionStatus
import com.sabeel.steps.domain.session.WalkLeg
import com.sabeel.steps.domain.session.WalkWithLegs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * JVM tests for the pure sessions export. CSV is read back with the production [CsvCodec] parser and
 * GPX is parsed with the JDK XML parser, so the assertions check real round-trips (escaped fields,
 * valid XML, point order) instead of string prefixes.
 */
class SessionExportTest {

    private val utc = ZoneOffset.UTC
    private val startedAt = Instant.parse("2026-11-22T10:00:00Z").toEpochMilli()
    private val namespace = SessionExport.GPX_NAMESPACE

    @Test
    fun csvStartsWithTheFixedHeaderAndColumnOrder() {
        val text = SessionExport.buildSessionCsv(emptyList(), utc)
        assertEquals(SessionExport.CSV_HEADER + "\n", text)
        assertEquals(18, SessionExport.CSV_HEADER_FIELDS.size)
        assertEquals(
            listOf(
                "id", "date", "startedAt", "endedAt", "activity", "status", "steps",
                "distanceMeters", "durationSec", "secondsPerKm", "goalType", "goalValue",
                "targetMet", "gpsUsed", "startPlaceId", "endPlaceId", "presetId", "legsCount",
            ),
            SessionExport.CSV_HEADER_FIELDS,
        )
    }

    @Test
    fun csvIsChronologicalWithTiesBrokenBySessionId() {
        val sessions = listOf(
            session(id = 1L, startedAtMillis = startedAt + 2_000L),
            session(id = 2L, startedAtMillis = startedAt),
            session(id = 3L, startedAtMillis = startedAt + 1_000L),
            session(id = 9L, startedAtMillis = startedAt),
            session(id = 4L, startedAtMillis = startedAt),
        )
        val rows = CsvCodec.parse(SessionExport.buildSessionCsv(sessions, utc)).drop(1)
        assertEquals(listOf("2", "4", "9", "3", "1"), rows.map { it[0] })
    }

    @Test
    fun csvRowContainsEveryColumnInOrder() {
        val legs = listOf(
            WalkLeg(date = "2026-11-21", steps = 100),
            WalkLeg(date = "2026-11-22", steps = 200),
        )
        val rows = CsvCodec.parse(SessionExport.buildSessionCsv(listOf(session(legs = legs)), utc))
        assertEquals(2, rows.size)
        val row = rows[1]
        assertEquals(18, row.size)
        assertEquals("7", row[0])
        assertEquals("2026-11-22", row[1])
        assertEquals(startedAt.toString(), row[2])
        assertEquals((startedAt + 1_800_000L).toString(), row[3])
        assertEquals("RUN", row[4])
        assertEquals("COMPLETED", row[5])
        assertEquals("4000", row[6])
        assertEquals("3000", row[7])
        assertEquals("1800", row[8])
        assertEquals("600", row[9])
        assertEquals("DISTANCE", row[10])
        assertEquals("5000", row[11])
        assertEquals("false", row[12])
        assertEquals("true", row[13])
        assertEquals("3", row[14])
        assertEquals("", row[15])
        assertEquals("5", row[16])
        assertEquals("2", row[17])
    }

    @Test
    fun csvQuotesFieldsContainingCommasQuotesAndNewlines() {
        assertEquals("\"a,b\"", SessionExport.escapeField("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", SessionExport.escapeField("say \"hi\""))
        assertEquals("\"line1\nline2\"", SessionExport.escapeField("line1\nline2"))
        assertEquals("plain", SessionExport.escapeField("plain"))
        // A built row stays one record even though the same escaping is used everywhere.
        assertEquals(18, CsvCodec.parse(SessionExport.buildSessionCsv(listOf(session()), utc))[1].size)
    }

    @Test
    fun csvFormatsDecimalsWithADotEvenInAForeignDefaultLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val row = CsvCodec.parse(
                SessionExport.buildSessionCsv(
                    listOf(session(distanceMeters = 1234.567, goalValue = 42.5)),
                    utc,
                ),
            )[1]
            assertEquals("1234.57", row[7])
            assertEquals("42.5", row[11])
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun csvNeverThrowsOnDegenerateInput() {
        val weird = session(
            id = 0L,
            startedAtMillis = 0L,
            endedAtMillis = -1L,
            durationSec = -5,
            steps = -3,
            distanceMeters = Double.NaN,
            goalValue = Double.POSITIVE_INFINITY,
            startPlaceId = null,
            endPlaceId = null,
            sessionPresetId = null,
        )
        val rows = CsvCodec.parse(SessionExport.buildSessionCsv(listOf(weird), utc))
        assertEquals(2, rows.size)
        assertEquals("0", rows[1][7])
        assertEquals("0", rows[1][11])
        assertEquals("", rows[1][14])
        assertEquals("", rows[1][15])
        assertEquals("", rows[1][16])
    }

    @Test
    fun gpxIsWellFormedSingleTrackGpx11() {
        val document = parseGpx(SessionExport.buildSessionGpx(session(route = route()), "Morning run"))
        val root = document.documentElement
        assertEquals("gpx", root.nodeName)
        assertEquals("1.1", root.getAttribute("version"))
        assertEquals(namespace, root.namespaceURI)
        assertEquals(1, document.getElementsByTagNameNS(namespace, "trk").length)
        assertEquals(3, document.getElementsByTagNameNS(namespace, "trkpt").length)
    }

    @Test
    fun gpxWritesRoutePointsInRecordedOrderWithUtcTimes() {
        val document = parseGpx(SessionExport.buildSessionGpx(session(route = route()), "Morning run"))
        val points = document.getElementsByTagNameNS(namespace, "trkpt")
        assertEquals(3, points.length)
        val latitudes = (0 until points.length).map { (points.item(it) as Element).getAttribute("lat") }
        val longitudes = (0 until points.length).map { (points.item(it) as Element).getAttribute("lon") }
        assertEquals(listOf("33.300001", "33.300002", "33.300003"), latitudes)
        assertEquals(listOf("44.400001", "44.400002", "44.400003"), longitudes)
        val first = points.item(0) as Element
        assertEquals("2026-11-22T10:00:01Z", first.getElementsByTagNameNS(namespace, "time").item(0).textContent)
    }

    @Test
    fun gpxEscapesXmlInTheTrackName() {
        val name = "Walk \"A&B\" <fast> '1'"
        val text = SessionExport.buildSessionGpx(session(route = route()), name)
        assertTrue(text.contains("&amp;"))
        assertTrue(text.contains("&lt;"))
        assertTrue(text.contains("&quot;"))
        val track = parseGpx(text).getElementsByTagNameNS(namespace, "trk").item(0) as Element
        assertEquals(name, track.getElementsByTagNameNS(namespace, "name").item(0).textContent)
    }

    @Test
    fun gpxWithoutRouteIsValidAndExplainsWhy() {
        val legs = listOf(
            WalkLeg(date = "2026-11-21", steps = 1200, distanceMeters = 900.0, durationSec = 600),
            WalkLeg(date = "2026-11-22", steps = 800),
        )
        val text = SessionExport.buildSessionGpx(
            session(route = emptyList(), legs = legs, gpsUsed = false),
            "No-GPS walk",
        )
        val document = parseGpx(text)
        assertEquals(0, document.getElementsByTagNameNS(namespace, "trkpt").length)
        val track = document.getElementsByTagNameNS(namespace, "trk").item(0) as Element
        val desc = track.getElementsByTagNameNS(namespace, "desc")
        assertEquals(1, desc.length)
        val description = desc.item(0).textContent
        assertTrue(description.startsWith(SessionExport.NO_ROUTE_DESC))
        assertTrue(description.contains("2026-11-21 (1200 steps)"))
        assertTrue(description.contains("2026-11-22 (800 steps)"))
    }

    @Test
    fun gpxSkipsInvalidRoutePointsAndPointsWithoutTiming() {
        val route = listOf(
            RoutePoint(Double.NaN, 44.4, startedAt + 1_000L),
            RoutePoint(33.3, 44.4, startedAt + 2_000L),
            RoutePoint(91.0, 44.4, startedAt + 3_000L),
            RoutePoint(33.31, 44.4, 0L),
        )
        val document = parseGpx(SessionExport.buildSessionGpx(session(route = route), "Mixed"))
        val points = document.getElementsByTagNameNS(namespace, "trkpt")
        assertEquals(2, points.length)
        assertEquals(1, (points.item(0) as Element).getElementsByTagNameNS(namespace, "time").length)
        assertEquals(0, (points.item(1) as Element).getElementsByTagNameNS(namespace, "time").length)
    }

    @Test
    fun gpxSanitizesCharactersXmlCannotRepresent() {
        val text = SessionExport.buildSessionGpx(session(), "a\u0001b")
        val track = parseGpx(text).getElementsByTagNameNS(namespace, "trk").item(0) as Element
        assertEquals("a?b", track.getElementsByTagNameNS(namespace, "name").item(0).textContent)
    }

    @Test
    fun gpxMetadataUsesTheSessionStartAndTrackHasNoDescWhenThereIsARoute() {
        val document = parseGpx(SessionExport.buildSessionGpx(session(route = route()), "x"))
        val metadata = document.getElementsByTagNameNS(namespace, "metadata").item(0) as Element
        assertEquals("2026-11-22T10:00:00Z", metadata.getElementsByTagNameNS(namespace, "time").item(0).textContent)
        val track = document.getElementsByTagNameNS(namespace, "trk").item(0) as Element
        assertEquals(0, track.getElementsByTagNameNS(namespace, "desc").length)
    }

    @Test
    fun gpxNeverThrowsOnDegenerateInput() {
        val degenerate = session(
            id = 0L,
            startedAtMillis = Long.MAX_VALUE,
            route = listOf(RoutePoint(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Long.MAX_VALUE)),
        )
        val text = SessionExport.buildSessionGpx(degenerate, " ")
        assertTrue(text.startsWith("<?xml"))
        parseGpx(text)
        assertTrue(text.contains("Sabeel session"))
    }

    private fun route(): List<RoutePoint> = listOf(
        RoutePoint(33.300001, 44.400001, startedAt + 1_000L),
        RoutePoint(33.300002, 44.400002, startedAt + 61_000L),
        RoutePoint(33.300003, 44.400003, startedAt + 121_000L),
    )

    private fun parseGpx(text: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
    }

    private fun session(
        id: Long = 7L,
        startedAtMillis: Long = startedAt,
        endedAtMillis: Long = startedAt + 1_800_000L,
        durationSec: Int = 1800,
        steps: Int = 4000,
        distanceMeters: Double = 3000.0,
        secondsPerKm: Int = 600,
        route: List<RoutePoint> = emptyList(),
        legs: List<WalkLeg> = emptyList(),
        activityType: ActivityType = ActivityType.RUN,
        goalType: GoalType = GoalType.DISTANCE,
        goalValue: Double = 5000.0,
        targetMet: Boolean = false,
        status: SessionStatus = SessionStatus.COMPLETED,
        gpsUsed: Boolean = true,
        startPlaceId: Long? = 3L,
        endPlaceId: Long? = null,
        sessionPresetId: Long? = 5L,
    ): WalkWithLegs = WalkWithLegs(
        walk = Walk(
            id = id,
            startedAt = startedAtMillis,
            endedAt = endedAtMillis,
            durationSec = durationSec,
            steps = steps,
            distanceMeters = distanceMeters,
            secondsPerKm = secondsPerKm,
            route = route,
            activityType = activityType,
            goalType = goalType,
            goalValue = goalValue,
            targetMet = targetMet,
            status = status,
            gpsUsed = gpsUsed,
            startPlaceId = startPlaceId,
            endPlaceId = endPlaceId,
            sessionPresetId = sessionPresetId,
        ),
        legs = legs,
    )
}
