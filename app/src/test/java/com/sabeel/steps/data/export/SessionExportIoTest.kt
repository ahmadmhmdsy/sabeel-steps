package com.sabeel.steps.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * JVM tests for the only pure part of [SessionExportIo]: the GPX track name. The SAF write path needs
 * an Android runtime, so its behaviour (URI opening, empty history, IOException mapping) is kept thin
 * and reviewed rather than mocked; there is no Robolectric in this project and none is added here.
 */
class SessionExportIoTest {

    @Test
    fun sessionNameIncludesTheSessionId() {
        assertEquals("Sabeel session 42", sessionName(42L))
    }

    @Test
    fun sessionNameIsStableForTheSameSession() {
        assertEquals(sessionName(7L), sessionName(7L))
    }

    @Test
    fun sessionNamesAreUniquePerSession() {
        assertNotEquals(sessionName(1L), sessionName(2L))
    }

    @Test
    fun sessionNameFallsBackForANonPositiveId() {
        assertEquals("Sabeel session", sessionName(0L))
        assertEquals("Sabeel session", sessionName(-7L))
    }
}
