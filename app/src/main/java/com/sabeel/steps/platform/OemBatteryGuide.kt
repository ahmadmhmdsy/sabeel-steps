package com.sabeel.steps.platform

import com.sabeel.steps.R

/**
 * Manufacturer-specific battery instructions for the "Why is my count low?" screen.
 *
 * This is deliberately data, not magic: Android has no public API to ask whether the phone maker
 * restricts background work, so the guide is chosen from [android.os.Build.MANUFACTURER] and the
 * screen always shows the generic path as well. Every entry is checkable by a human on the phone;
 * Sabeel never requests the battery-optimization exemption intent, which would be a new permission
 * and a Play policy concern for a step counter.
 */
object OemBatteryGuide {

    data class Guide(
        /** Human-readable maker name shown in the section title (Latin name, never translated). */
        val brandLabel: String,
        /** String resources with the instruction(s), in the order they should be read. */
        val stepResIds: List<Int>,
    )

    fun guideFor(manufacturer: String?): Guide =
        Guide(brandLabel = brandLabel(manufacturer), stepResIds = stepResIds(manufacturer))

    fun brandLabel(manufacturer: String?): String = when (brandKey(manufacturer)) {
        "xiaomi" -> "Xiaomi"
        "samsung" -> "Samsung"
        "huawei" -> "Huawei"
        "honor" -> "Honor"
        "oneplus" -> "OnePlus"
        "oppo" -> "OPPO"
        "realme" -> "realme"
        "vivo" -> "vivo"
        "asus" -> "ASUS"
        "nokia" -> "Nokia"
        "motorola" -> "Motorola"
        "sony" -> "Sony"
        "lg" -> "LG"
        "google" -> "Google"
        "nothing" -> "Nothing"
        // Emulators and a few imports report "unknown": show the generic label rather than a raw
        // non-word in the localized UI.
        else -> manufacturer?.trim()?.takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) } ?: "Android"
    }

    fun stepResIds(manufacturer: String?): List<Int> = when (brandKey(manufacturer)) {
        "xiaomi" -> listOf(R.string.diagnostics_oem_xiaomi)
        "samsung" -> listOf(R.string.diagnostics_oem_samsung)
        "huawei", "honor" -> listOf(R.string.diagnostics_oem_huawei)
        "oneplus" -> listOf(R.string.diagnostics_oem_oneplus)
        "oppo", "realme" -> listOf(R.string.diagnostics_oem_oppo)
        "vivo" -> listOf(R.string.diagnostics_oem_vivo)
        else -> listOf(R.string.diagnostics_oem_generic)
    }

    /** Manufacturer aliases share one guide so Redmi/POCO phones get the Xiaomi path, not the generic one. */
    private fun brandKey(manufacturer: String?): String = when (val key = manufacturer.orEmpty().trim().lowercase()) {
        "redmi", "poco" -> "xiaomi"
        "lge" -> "lg"
        else -> key
    }
}
