package com.sabeel.steps.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sabeel.steps.R
import com.sabeel.steps.domain.DateKey
import com.sabeel.steps.domain.StepMath
import com.sabeel.steps.domain.Units
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Locale-aware presentation helpers. The arithmetic stays in domain/StepMath; only the wording of
 * units and dates lives here so Arabic reads naturally instead of showing English unit strings.
 */

@Composable
fun distanceText(meters: Double, units: Units): String {
    val safe = meters.coerceAtLeast(0.0)
    return if (units == Units.METRIC) {
        if (safe < 1000.0) {
            withUnit(safe.roundToInt().toString(), stringResource(R.string.unit_meter))
        } else {
            withUnit(String.format(Locale.US, "%.1f", safe / 1000.0), stringResource(R.string.unit_kilometer))
        }
    } else {
        val miles = safe / 1609.344
        if (miles < 0.1) {
            withUnit((safe * 3.28084).roundToInt().toString(), stringResource(R.string.unit_foot))
        } else {
            withUnit(String.format(Locale.US, "%.1f", miles), stringResource(R.string.unit_mile))
        }
    }
}

@Composable
fun paceText(secondsPerKm: Int, units: Units): String {
    if (secondsPerKm <= 0) return stringResource(R.string.value_none)
    val scaled = if (units == Units.METRIC) secondsPerKm.toDouble() else secondsPerKm * 1.609344
    val total = scaled.roundToInt()
    val pace = String.format(Locale.US, "%d:%02d", total / 60, total % 60)
    val suffix = stringResource(
        if (units == Units.METRIC) R.string.pace_suffix_km else R.string.pace_suffix_mile,
    )
    return withUnit(pace, suffix)
}

@Composable
fun caloriesText(kcal: Double): String =
    withUnit(kcal.coerceAtLeast(0.0).roundToInt().toString(), stringResource(R.string.unit_kcal))

@Composable
fun stepsText(steps: Int): String = StepMath.formatSteps(steps)

@Composable
private fun withUnit(value: String, unit: String): String =
    stringResource(R.string.value_with_unit, value, unit)

@Composable
fun percentText(fraction: Float): String =
    stringResource(R.string.percent_value, (fraction.coerceIn(0f, 1f) * 100f).roundToInt())

@Composable
fun dayLabel(date: String): String {
    val today = DateKey.today()
    if (date == today) return stringResource(R.string.day_today)
    if (date == DateKey.minusDays(today, 1)) return stringResource(R.string.day_yesterday)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM", locale) }
    return runCatching { LocalDate.parse(date).format(formatter) }.getOrDefault(date)
}

@Composable
fun shortDayLabel(date: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    return runCatching { LocalDate.parse(date).format(formatter) }.getOrDefault("")
}

@Composable
fun timeOfDayText(minutes: Int): String {
    val context = LocalContext.current
    val safeMinutes = minutes.coerceIn(0, 23 * 60 + 59)
    val time = LocalTime.of(safeMinutes / 60, safeMinutes % 60)
    val millis = remember(safeMinutes) {
        time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val formatter = remember(context) { android.text.format.DateFormat.getTimeFormat(context) }
    return remember(millis, formatter) { formatter.format(Date(millis)) }
}
