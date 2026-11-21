package com.sabeel.steps.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Thin, read-only adapter over the Health Connect client.
 *
 * Contract:
 *  - the app requests exactly one permission (read steps) and never writes health data;
 *  - every platform call is defensive: a missing/updating provider or a revoked grant degrades to
 *    [Availability.UNAVAILABLE]/[Availability.PERMISSION_NEEDED], never to a crash;
 *  - [readToday] returns the provider's absolute "steps since midnight" total, which the pipeline
 *    consumes as a floor (never as an additive delta), so polling cannot double count.
 *
 * Health Connect may be absent (most emulators, some regions). Callers must check [availability]
 * before showing the opt-in and must treat it as optional.
 */
object HealthConnect {

    /** Package that implements the Health Connect provider. */
    const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

    /** Manifest permission the user grants inside Health Connect. */
    const val READ_STEPS_PERMISSION = "android.permission.health.READ_STEPS"

    /** The permission every counting read needs. */
    val readPermissions: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    /** A hung provider must never block the poll loop; after this the floor is simply not updated. */
    const val READ_TIMEOUT_MS = 8_000L

    /**
     * The permission set the rationale/request flows ask for. The background-read grant is added only
     * when the provider advertises [HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND]:
     * requesting a permission the installed provider does not know can make the whole request fail.
     */
    fun requiredPermissions(context: Context): Set<String> {
        val base = readPermissions
        return if (supportsBackgroundRead(context)) {
            base + HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        } else {
            base
        }
    }

    /** Whether this provider supports Health Connect reads while the app is in the background. */
    fun supportsBackgroundRead(context: Context): Boolean = runCatching {
        val client = HealthConnectClient.getOrCreate(context)
        client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }.getOrDefault(false)

    fun hasBackgroundReadPermission(context: Context): Boolean = runCatching {
        ContextCompat.checkSelfPermission(
            context,
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
        ) == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    enum class Availability {
        /** The provider is missing or needs an update; no Health Connect UI makes sense. */
        UNAVAILABLE,

        /** The provider is usable and the user has not granted step reads yet. */
        PERMISSION_NEEDED,

        /** The provider is usable and step reads are granted. */
        READY,
    }

    /** Absolute steps recorded since local midnight, as Health Connect reports them. */
    data class Today(val steps: Int)

    fun availability(context: Context): Availability = runCatching {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                if (hasPermission(context)) Availability.READY else Availability.PERMISSION_NEEDED
            }
            else -> Availability.UNAVAILABLE
        }
    }.getOrDefault(Availability.UNAVAILABLE)

    fun hasPermission(context: Context): Boolean = runCatching {
        ContextCompat.checkSelfPermission(context, READ_STEPS_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /**
     * True when Health Connect can be offered as a counting source on this phone: the provider is
     * present and the grant exists. The "no hardware step sensor" part of the decision belongs to
     * [com.sabeel.steps.domain.SourceAvailability].
     */
    fun isReady(context: Context): Boolean = availability(context) == Availability.READY

    /** Opens the Health Connect screens where the user manages the grant. */
    fun settingsIntent(context: Context): Intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Reads today's absolute total, or null when the read genuinely failed or timed out (the caller
     * keeps its existing floor; it never lowers the day). The timeout protects the poll loop from a
     * hung provider binder: a cancelled aggregate simply returns null and the next poll tries again.
     */
    suspend fun readToday(context: Context, nowMillis: Long = System.currentTimeMillis()): Today? =
        runCatching {
            withTimeoutOrNull(READ_TIMEOUT_MS) {
                val client = HealthConnectClient.getOrCreate(context)
                val zone = ZoneId.systemDefault()
                val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
                val response = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(
                            Instant.ofEpochMilli(startOfDay),
                            Instant.ofEpochMilli(nowMillis),
                        ),
                    ),
                )
                Today(steps = (response[StepsRecord.COUNT_TOTAL] ?: 0L).coerceAtLeast(0L).toInt())
            }
        }.getOrNull()
}
