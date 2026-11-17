package com.sabeel.steps.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sabeel.steps.MainActivity
import com.sabeel.steps.R
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.ui.i18n.AppLocale

/**
 * Single owner of notification channels and of every notification Sabeel ever posts.
 *
 * Channels: steps (low, silent), reminders (default), goal (default). Every channel name and every
 * notification body is built from an [AppLocale.wrap] context, so the app language chosen in
 * Settings (or the device locale) is what the system notification shade shows. [ensureChannels] is
 * idempotent and re-creates the channels on every call, which is what updates their names in system
 * settings after a language change while preserving the user's per-channel choices.
 *
 * The goal notification is posted at most once per local day; the date of the last post is kept in
 * a private SharedPreferences file (never leaves the device).
 */
object Notifications {

    const val CHANNEL_STEPS = "steps"
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_GOAL = "goal"

    const val ID_STEP_SERVICE = 4101
    const val ID_WALK = 4102
    const val ID_GOAL = 4103
    const val ID_REMINDER = 4104

    private const val PREFS = "sabeel_notifications"
    private const val KEY_GOAL_NOTIFIED_DATE = "goal_notified_date"
    private const val ACCENT_COLOR = 0xFF0F6B5C.toInt()

    /**
     * Creates or refreshes every channel in the active language. Safe to call any number of times,
     * from any component. On API 26+ the framework updates an existing channel's name/description
     * (user-visible settings) without touching the user's importance/sound choices.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val localized = AppLocale.wrap(context)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STEPS,
                localized.getString(R.string.sensor_notif_channel_steps),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = localized.getString(R.string.sensor_notif_channel_steps_desc)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                localized.getString(R.string.sensor_notif_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = localized.getString(R.string.sensor_notif_channel_reminders_desc)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GOAL,
                localized.getString(R.string.sensor_notif_channel_goal),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = localized.getString(R.string.sensor_notif_channel_goal_desc)
                setShowBadge(false)
            },
        )
    }

    /** Delete-all: forget the once-per-day goal flag so a fresh install starts clean. */
    fun resetPrefs(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** Persistent, silent notification of the step-counting foreground service. */
    fun stepServiceNotification(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_STEPS)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    /** Foreground notification of the walk service, with a Stop action. */
    fun walkNotification(
        context: Context,
        title: String,
        text: String,
        stopAction: PendingIntent,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_STEPS)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_stat_steps,
                AppLocale.wrap(context).getString(R.string.sensor_notif_walk_stop),
                stopAction,
            )
            .build()

    /**
     * Posts the "daily goal reached" notification at most once per local day.
     *
     * The once-per-day flag is written only after the post actually succeeded and only when
     * POST_NOTIFICATIONS is granted (and the channel is not switched off by the user): a denied or
     * failed post must not consume today's congratulation, because the user may grant the permission
     * later the same day.
     *
     * @return true when a notification was actually posted now.
     */
    fun notifyGoalReached(context: Context, steps: Int, goalSteps: Int): Boolean {
        if (goalSteps <= 0 || steps < goalSteps) return false
        if (!Permissions.hasNotifications(context)) return false
        val prefs = prefs(context)
        val today = DateKey.today()
        if (prefs.getString(KEY_GOAL_NOTIFIED_DATE, null) == today) return false

        ensureChannels(context)
        if (isChannelOff(context, CHANNEL_GOAL)) return false

        val localized = AppLocale.wrap(context)
        val text = localized.getString(R.string.sensor_notif_goal_text, steps, goalSteps)
        val notification = NotificationCompat.Builder(context, CHANNEL_GOAL)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setColor(ACCENT_COLOR)
            .setContentTitle(localized.getString(R.string.sensor_notif_goal_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val posted = post(context, ID_GOAL, notification)
        if (posted) prefs.edit().putString(KEY_GOAL_NOTIFIED_DATE, today).apply()
        return posted
    }

    /**
     * Posts the user's daily walk reminder. Permission-gated and localized; silently does nothing
     * when POST_NOTIFICATIONS is denied or the reminder channel is off.
     *
     * @return true when the post was attempted successfully.
     */
    fun notifyReminder(context: Context): Boolean {
        if (!Permissions.hasNotifications(context)) return false
        ensureChannels(context)
        if (isChannelOff(context, CHANNEL_REMINDERS)) return false
        val localized = AppLocale.wrap(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setColor(ACCENT_COLOR)
            .setContentTitle(localized.getString(R.string.sensor_reminder_title))
            .setContentText(localized.getString(R.string.sensor_reminder_text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return post(context, ID_REMINDER, notification)
    }

    /**
     * The single permission-checked post path for every notification in the app. Lint's
     * MissingPermission check has to be satisfied where notify() is called, so the services use
     * this helper instead of calling the compat API directly; when the notification permission is
     * denied nothing is posted and false is returned.
     */
    fun post(context: Context, id: Int, notification: Notification): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return runCatching { NotificationManagerCompat.from(context).notify(id, notification) }.isSuccess
    }

    /** True when the user switched this channel off; posting into it would be invisible anyway. */
    private fun isChannelOff(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return true
        val channel = manager.getNotificationChannel(channelId) ?: return true
        return channel.importance == NotificationManager.IMPORTANCE_NONE
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            ID_STEP_SERVICE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
