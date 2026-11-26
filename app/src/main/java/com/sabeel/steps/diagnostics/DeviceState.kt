package com.sabeel.steps.diagnostics

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.sabeel.steps.platform.Permissions

/**
 * The device state a support report needs, read without touching the network or asking for new
 * permissions. Every field degrades to a null/unknown value instead of throwing: a report is worth
 * more than a perfect one.
 */
data class DeviceState(
    val batteryPercent: Int = -1,
    val charging: Boolean = false,
    val screenOn: Boolean = true,
    val dozeMode: Boolean = false,
    val powerSave: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val appStandbyBucket: Int? = null,
    val backgroundRestricted: Boolean? = null,
    val notificationsAllowed: Boolean = true,
    val activityRecognition: Boolean = true,
) {
    fun toFields(): Map<String, Any?> = mapOf(
        "bat" to batteryPercent,
        "chg" to charging,
        "screen" to screenOn,
        "doze" to dozeMode,
        "psave" to powerSave,
        "exempt" to batteryUnrestricted,
        "bucket" to appStandbyBucket,
        "bgrestricted" to backgroundRestricted,
        "notif" to notificationsAllowed,
        "actrec" to activityRecognition,
    )
}

object DeviceStateReader {

    fun read(context: Context): DeviceState {
        val app = context.applicationContext
        val power = runCatching { app.getSystemService(PowerManager::class.java) }.getOrNull()
        val battery = runCatching { app.getSystemService(BatteryManager::class.java) }.getOrNull()
        val bucket = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                app.getSystemService(UsageStatsManager::class.java)?.appStandbyBucket
            } else {
                null
            }
        }.getOrNull()
        val backgroundRestricted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                app.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted
            } else {
                null
            }
        }.getOrNull()
        return DeviceState(
            batteryPercent = runCatching { battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1 }
                .getOrDefault(-1),
            charging = runCatching { battery?.isCharging == true }.getOrDefault(false),
            screenOn = runCatching { power?.isInteractive != false }.getOrDefault(true),
            dozeMode = runCatching { power?.isDeviceIdleMode == true }.getOrDefault(false),
            powerSave = runCatching { power?.isPowerSaveMode == true }.getOrDefault(false),
            batteryUnrestricted = runCatching {
                power?.isIgnoringBatteryOptimizations(app.packageName) == true
            }.getOrDefault(false),
            appStandbyBucket = bucket,
            backgroundRestricted = backgroundRestricted,
            notificationsAllowed = Permissions.hasNotifications(app),
            activityRecognition = Permissions.hasActivityRecognition(app),
        )
    }
}
