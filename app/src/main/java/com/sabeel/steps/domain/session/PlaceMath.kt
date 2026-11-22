package com.sabeel.steps.domain.session

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure distance and geofence math for local places (council amendments A9/A10: places are a local
 * label + coordinates + radius; no geocoding, no map tiles, no network). No Android imports, so the
 * radius and nearest-place rules are unit-tested on the JVM.
 *
 * Coordinates are WGS-84 decimal degrees; invalid coordinates never throw, they are treated as
 * "infinitely far away" so a bad sensor fix can never report "you have arrived".
 */
object PlaceMath {

    /** Mean Earth radius (IUGG), metres. */
    const val EARTH_RADIUS_METERS = 6_371_008.8

    /** True when both values are finite and inside the legal WGS-84 ranges. */
    fun isValidCoordinate(lat: Double, lng: Double): Boolean =
        lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0

    /**
     * Great-circle distance in metres between two WGS-84 coordinates (haversine formula).
     *
     * Returns [Double.POSITIVE_INFINITY] for invalid input instead of throwing or returning a
     * misleading 0: callers use the value for geofence checks and "nearest place" ordering, where
     * infinite is the only safe answer for a fix we cannot trust.
     */
    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if (!isValidCoordinate(lat1, lng1) || !isValidCoordinate(lat2, lng2)) {
            return Double.POSITIVE_INFINITY
        }
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val sinLat = sin(dLat / 2.0)
        val sinLng = sin(dLng / 2.0)
        val a = (sinLat * sinLat +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sinLng * sinLng)
            .coerceIn(0.0, 1.0)
        return 2.0 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1.0 - a))
    }

    /**
     * True when (lat, lng) is inside [place]'s radius. The boundary is inclusive and a radius of 0
     * only matches the exact centre point. An invalid place or fix is never inside.
     */
    fun isInside(place: Place, lat: Double, lng: Double): Boolean {
        if (!place.radiusM.isFinite()) return false
        val radius = place.radiusM.coerceAtLeast(0.0)
        val distance = haversineMeters(place.lat, place.lon, lat, lng)
        return distance.isFinite() && distance <= radius
    }

    /**
     * Nearest place to (lat, lng), or null when [places] is empty. Ties keep the earliest entry in
     * list order, so the UI list order is the tie-breaker and the result is deterministic.
     */
    fun nearestPlace(places: List<Place>, lat: Double, lng: Double): Place? {
        var best: Place? = null
        var bestDistance = Double.POSITIVE_INFINITY
        for (place in places) {
            val distance = haversineMeters(place.lat, place.lon, lat, lng)
            if (distance < bestDistance) {
                best = place
                bestDistance = distance
            }
        }
        return best
    }
}
