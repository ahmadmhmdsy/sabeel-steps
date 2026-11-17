package com.sabeel.steps.ui.i18n

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import com.sabeel.steps.domain.AppLanguage
import java.util.Locale

/**
 * In-app language switch without AppCompat (the app does not depend on it).
 *
 * [MainActivity.attachBaseContext] calls [wrap], which returns a context whose configuration carries
 * the chosen locale and its layout direction; Compose then lays every screen out RTL/LTR correctly.
 * Reading is a synchronous SharedPreferences get because attachBaseContext cannot wait for DataStore.
 * The DataStore copy of the language (SettingsRepository) is kept in sync by SettingsViewModel so the
 * domain layer and the platform layer agree on the choice.
 *
 * Until the user makes an explicit choice, the device language decides (decision D7): an Arabic
 * phone starts in Arabic and right-to-left, everything else starts in English.
 */
object AppLocale {
    private const val PREFS = "sabeel_locale"
    private const val KEY = "language"

    fun current(base: Context): AppLanguage {
        val stored = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (stored != null) return AppLanguage.fromCode(stored)
        return deviceDefault(base)
    }

    /** Removes the explicit choice; the next [current] call falls back to the device language. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun wrap(base: Context): Context {
        val language = current(base)
        val locale = Locale.forLanguageTag(language.code)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** Persists synchronously and recreates the activity so resources reload with the new locale. */
    fun apply(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, language.code)
            .apply()
        context.findActivity()?.recreate()
    }

    private fun deviceDefault(base: Context): AppLanguage {
        val language = base.resources.configuration.locales[0].language
        return if (language == "ar") AppLanguage.AR else AppLanguage.EN
    }

    private fun Context.findActivity(): Activity? {
        var cursor: Context? = this
        while (cursor is ContextWrapper) {
            if (cursor is Activity) return cursor
            cursor = cursor.baseContext
        }
        return null
    }
}
