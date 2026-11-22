package com.sabeel.steps.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the pure place math. Coordinates are WGS-84 decimal degrees and distances are
 * compared with generous absolute tolerances so the tests pin behaviour (inside/outside, boundary
 * inclusivity, tie order) and not the last floating-point bit of the haversine formula.
 */
class PlaceMathTest {

    private val home = Place(id = 1L, label = "Home", lat = 33.3, lon = 44.4, radiusM = 100.0, createdAt = 0L)
    private val work = Place(id = 2L, label = "Work", lat = 33.31, lon = 44.4, radiusM = 100.0, createdAt = 0L)

    @Test
    fun identicalPointsAreZeroMetresApart() {
        assertEquals(0.0, PlaceMath.haversineMeters(33.3, 44.4, 33.3, 44.4), 1e-9)
    }

    @Test
    fun distanceIsSymmetricAndAboutOneKilometreForOneHundredthOfADegree() {
        val there = PlaceMath.haversineMeters(33.3, 44.4, 33.31, 44.4)
        val back = PlaceMath.haversineMeters(33.31, 44.4, 33.3, 44.4)
        assertEquals(there, back, 1e-6)
        assertTrue("expected about 1.1 km, was " + there, there in 1_100.0..1_125.0)
    }

    @Test
    fun oneDegreeOfLongitudeAtTheEquatorIsAbout111Kilometres() {
        val distance = PlaceMath.haversineMeters(0.0, 0.0, 0.0, 1.0)
        assertTrue("expected about 111.3 km, was " + distance, distance in 111_000.0..111_700.0)
    }

    @Test
    fun longitudeWrapUsesTheShortestPath() {
        val distance = PlaceMath.haversineMeters(0.0, 179.9, 0.0, -179.9)
        assertTrue("expected about 22.2 km across the date line, was " + distance, distance in 22_000.0..22_500.0)
    }

    @Test
    fun invalidCoordinatesAreInfinitelyFarAway() {
        assertTrue(PlaceMath.haversineMeters(Double.NaN, 44.4, 33.3, 44.4).isInfinite())
        assertTrue(PlaceMath.haversineMeters(91.0, 44.4, 33.3, 44.4).isInfinite())
        assertTrue(PlaceMath.haversineMeters(33.3, -190.0, 33.3, 44.4).isInfinite())
        assertTrue(PlaceMath.haversineMeters(33.3, 44.4, Double.POSITIVE_INFINITY, 44.4).isInfinite())
    }

    @Test
    fun coordinateValidationCoversTheLegalRanges() {
        assertTrue(PlaceMath.isValidCoordinate(0.0, 0.0))
        assertTrue(PlaceMath.isValidCoordinate(90.0, 180.0))
        assertTrue(PlaceMath.isValidCoordinate(-90.0, -180.0))
        assertFalse(PlaceMath.isValidCoordinate(90.000001, 0.0))
        assertFalse(PlaceMath.isValidCoordinate(0.0, 180.000001))
        assertFalse(PlaceMath.isValidCoordinate(Double.NaN, 0.0))
        assertFalse(PlaceMath.isValidCoordinate(0.0, Double.NaN))
    }

    @Test
    fun theCentreOfAPlaceIsInsideIt() {
        assertTrue(PlaceMath.isInside(home, home.lat, home.lon))
    }

    @Test
    fun insideAndOutsideFollowTheConfiguredRadius() {
        val fiftyMetresNorth = home.lat + metresToLatDegrees(50.0)
        val oneHundredFiftyMetresNorth = home.lat + metresToLatDegrees(150.0)
        assertTrue(PlaceMath.isInside(home, fiftyMetresNorth, home.lon))
        assertFalse(PlaceMath.isInside(home, oneHundredFiftyMetresNorth, home.lon))
    }

    @Test
    fun theRadiusBoundaryIsInclusive() {
        val edgeLat = home.lat + metresToLatDegrees(80.0)
        val exactDistance = PlaceMath.haversineMeters(home.lat, home.lon, edgeLat, home.lon)
        assertTrue(PlaceMath.isInside(home.copy(radiusM = exactDistance), edgeLat, home.lon))
        assertFalse(PlaceMath.isInside(home.copy(radiusM = exactDistance - 0.001), edgeLat, home.lon))
    }

    @Test
    fun zeroRadiusOnlyMatchesTheExactCentrePoint() {
        val dot = home.copy(radiusM = 0.0)
        assertTrue(PlaceMath.isInside(dot, dot.lat, dot.lon))
        assertFalse(PlaceMath.isInside(dot, dot.lat + 1e-9, dot.lon))
    }

    @Test
    fun negativeRadiusIsTreatedAsZero() {
        val negative = home.copy(radiusM = -50.0)
        assertTrue(PlaceMath.isInside(negative, negative.lat, negative.lon))
        assertFalse(PlaceMath.isInside(negative, negative.lat + metresToLatDegrees(10.0), negative.lon))
    }

    @Test
    fun anInvalidFixIsNeverInside() {
        assertFalse(PlaceMath.isInside(home, Double.NaN, home.lon))
        assertFalse(PlaceMath.isInside(home, home.lat, Double.POSITIVE_INFINITY))
        assertFalse(PlaceMath.isInside(home, 91.0, home.lon))
    }

    @Test
    fun anInvalidPlaceNeverContainsAnything() {
        assertFalse(PlaceMath.isInside(home.copy(lat = Double.NaN), home.lat, home.lon))
        assertFalse(PlaceMath.isInside(home.copy(radiusM = Double.NaN), home.lat, home.lon))
    }

    @Test
    fun nearestPlacePicksTheClosestEntryRegardlessOfListOrder() {
        val nearWork = work.lat + 0.0001
        assertSame(work, PlaceMath.nearestPlace(listOf(home, work), nearWork, work.lon))
        assertSame(work, PlaceMath.nearestPlace(listOf(work, home), nearWork, work.lon))
    }

    @Test
    fun nearestPlaceTiesKeepTheFirstListEntry() {
        val copy = home.copy(id = 9L, label = "Home copy")
        assertSame(home, PlaceMath.nearestPlace(listOf(home, copy), home.lat, home.lon))
    }

    @Test
    fun nearestPlaceWithNoPlacesIsNull() {
        assertNull(PlaceMath.nearestPlace(emptyList(), 33.3, 44.4))
    }

    @Test
    fun nearestPlaceIgnoresInvalidPlaceCoordinates() {
        val broken = home.copy(id = 7L, lat = Double.NaN)
        assertSame(home, PlaceMath.nearestPlace(listOf(broken, home), home.lat, home.lon))
        assertNull(PlaceMath.nearestPlace(listOf(broken), home.lat, home.lon))
    }

    private fun metresToLatDegrees(metres: Double): Double =
        Math.toDegrees(metres / PlaceMath.EARTH_RADIUS_METERS)
}
