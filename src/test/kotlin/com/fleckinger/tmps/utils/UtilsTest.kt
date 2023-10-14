package com.fleckinger.tmps.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DateTimeException
import java.time.ZoneId

class UtilsTest {

    @Test
    fun getTimezoneId_shouldThrowDateTimeException_ifZoneNotFoundByOffset() {
        assertThrows<DateTimeException> { Utils.getTimezoneId(352) }
    }

    @Test
    fun getTimezoneId_shouldReturnZoneIdOfAden() {
        val zoneId = ZoneId.of("Asia/Aden")
        assertEquals(zoneId, Utils.getTimezoneId(3))
    }

    @Test
    fun getTimezoneId_shouldReturnZoneIdOfYerevan() {
        val zoneId = ZoneId.of("Asia/Yerevan")
        assertEquals(zoneId, Utils.getTimezoneId(4))
    }
}