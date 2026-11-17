package com.sabeel.steps.platform

import com.sabeel.steps.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The diagnostics screen must never show a raw or nonsensical maker label, and brands that share a
 * battery UI must share one guide. These are pure JVM checks: no Android runtime is needed to read
 * the mapping, only the generated R ids.
 */
class OemBatteryGuideTest {

    @Test
    fun knownBrandsGetTheirOwnGuide() {
        assertEquals("Xiaomi", OemBatteryGuide.brandLabel("Xiaomi"))
        assertEquals(listOf(R.string.diagnostics_oem_xiaomi), OemBatteryGuide.stepResIds("Xiaomi"))

        assertEquals("Samsung", OemBatteryGuide.brandLabel("samsung"))
        assertEquals(listOf(R.string.diagnostics_oem_samsung), OemBatteryGuide.stepResIds("samsung"))

        assertEquals("OnePlus", OemBatteryGuide.brandLabel("OnePlus"))
        assertEquals(listOf(R.string.diagnostics_oem_oneplus), OemBatteryGuide.stepResIds("OnePlus"))

        assertEquals("vivo", OemBatteryGuide.brandLabel("vivo"))
        assertEquals(listOf(R.string.diagnostics_oem_vivo), OemBatteryGuide.stepResIds("vivo"))
    }

    @Test
    fun siblingBrandsShareOneGuide() {
        assertEquals("Honor", OemBatteryGuide.brandLabel("HONOR"))
        assertEquals(listOf(R.string.diagnostics_oem_huawei), OemBatteryGuide.stepResIds("HONOR"))

        assertEquals("realme", OemBatteryGuide.brandLabel("realme"))
        assertEquals(listOf(R.string.diagnostics_oem_oppo), OemBatteryGuide.stepResIds("realme"))
    }

    @Test
    fun aliasesMapToTheirFamily() {
        assertEquals("Xiaomi", OemBatteryGuide.brandLabel("Redmi"))
        assertEquals(listOf(R.string.diagnostics_oem_xiaomi), OemBatteryGuide.stepResIds("POCO"))

        assertEquals("LG", OemBatteryGuide.brandLabel("LGE"))
        assertEquals(listOf(R.string.diagnostics_oem_generic), OemBatteryGuide.stepResIds("LGE"))
    }

    @Test
    fun unknownOrBlankFallsBackToTheGenericGuide() {
        assertEquals("Android", OemBatteryGuide.brandLabel("unknown"))
        assertEquals("Android", OemBatteryGuide.brandLabel("UNKNOWN"))
        assertEquals("Android", OemBatteryGuide.brandLabel(null))
        assertEquals("Android", OemBatteryGuide.brandLabel("   "))

        assertEquals(listOf(R.string.diagnostics_oem_generic), OemBatteryGuide.stepResIds("unknown"))
        assertEquals(listOf(R.string.diagnostics_oem_generic), OemBatteryGuide.stepResIds(null))
    }

    @Test
    fun unusualMakerIsShownAsIs() {
        assertEquals("Fairphone", OemBatteryGuide.brandLabel("Fairphone"))
        assertEquals(listOf(R.string.diagnostics_oem_generic), OemBatteryGuide.stepResIds("Fairphone"))
    }
}
