package com.sabeel.steps.domain

/** Persisted sensor pipeline state (lives in DataStore together with user settings). */
data class StepState(
    val baselineRaw: Float,
    val todaySteps: Int,
    val baselineDate: String,
) {
    companion object {
        fun initial(date: String) = StepState(baselineRaw = 0f, todaySteps = 0, baselineDate = date)
    }
}

data class StepUpdate(
    val state: StepState,
    val todaySteps: Int,
    val rolledOver: Boolean,
)

/**
 * Reconciles the cumulative hardware step counter with the local day.
 * Pure function so the tricky cases (reboot, process death, midnight) are unit-testable.
 */
object StepPipeline {

    fun onSensor(state: StepState, raw: Float, today: String): StepUpdate {
        var baselineRaw = state.baselineRaw
        // Counter reset (reboot / sensor reset): the raw value went backwards -> rebase, count nothing.
        if (raw < baselineRaw - 1f) baselineRaw = raw
        val delta = (raw - baselineRaw).coerceAtLeast(0f).toInt()
        var todayBase = state.todaySteps
        var rolled = false
        if (today != state.baselineDate) {
            // First event after midnight: steps since the previous event belong to the new day.
            rolled = true
            todayBase = 0
        }
        val todaySteps = (todayBase + delta).coerceAtLeast(0)
        return StepUpdate(
            state = StepState(baselineRaw = raw, todaySteps = todaySteps, baselineDate = today),
            todaySteps = todaySteps,
            rolledOver = rolled,
        )
    }
}