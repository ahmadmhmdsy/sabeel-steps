package com.sabeel.steps.service

import com.sabeel.steps.domain.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-local live walk state, written by WalkTrackingService and observed by the Walk screen.
 * Contract: the service is the only writer; the UI only collects [state].
 */
object WalkTracker {
    data class Live(
        val running: Boolean = false,
        val startedAt: Long = 0L,
        val elapsedSec: Int = 0,
        val distanceMeters: Double = 0.0,
        val steps: Int = 0,
        val secondsPerKm: Int = 0,
        val route: List<RoutePoint> = emptyList(),
        val gpsActive: Boolean = false,
        val saving: Boolean = false,
        val savedWalkId: Long? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(Live())
    val state: StateFlow<Live> = _state.asStateFlow()

    fun update(transform: (Live) -> Live) {
        _state.update(transform)
    }

    fun reset() {
        _state.value = Live()
    }
}
