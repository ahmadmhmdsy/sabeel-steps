package com.sabeel.steps

import android.app.Application
import com.sabeel.steps.diagnostics.FlightRecorder
import com.sabeel.steps.diagnostics.PostMortem

class SabeelApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Evidence comes first: app-open resume rewrites the service heartbeat, so the previous
        // process's last known state and its exit reason must be captured before AppContainer starts
        // anything. Both calls are no-ops in a public build (BuildConfig.FIELD_DIAGNOSTICS = false).
        if (BuildConfig.FIELD_DIAGNOSTICS) {
            FlightRecorder.install(this)
            PostMortem.capture(this)
        }
        container = AppContainer(this)
    }
}
