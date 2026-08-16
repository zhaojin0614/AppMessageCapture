package com.aifactory.appmessagecapture.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tests for [RecurringBillProcessor.nextDueDateAfter] (production code).
 *
 * Covers the calendar boundaries that previously had zero coverage:
 * month-end clamping (Jan 31 -> Feb 28), leap years, and the default
 * frequency fallback.
 */
class RecurringScheduleTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun millisOf(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun next(from: LocalDate, frequency: String): LocalDate {
        val nextMillis = RecurringBillProcessor.nextDueDateAfter(millisOf(from), frequency, zone)
        return java.time.Instant.ofEpochMilli(nextMillis).atZone(zone).toLocalDate()
    }

    @Test
    fun `monthly from january 31 clamps to february 28 in non-leap year`() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            next(LocalDate.of(2025, 1, 31), RecurringFrequency.MONTHLY.name)
        )
    }

    @Test
    fun `monthly from january 31 lands on february 29 in leap year`() {
        assertEquals(
            LocalDate.of(2024, 2, 29),
            next(LocalDate.of(2024, 1, 31), RecurringFrequency.MONTHLY.name)
        )
    }

    @Test
    fun `monthly from february 29 in leap year lands on march 29`() {
        assertEquals(
            LocalDate.of(2024, 3, 29),
            next(LocalDate.of(2024, 2, 29), RecurringFrequency.MONTHLY.name)
        )
    }

    @Test
    fun `daily advances one day across month boundary`() {
        assertEquals(
            LocalDate.of(2025, 3, 1),
            next(LocalDate.of(2025, 2, 28), RecurringFrequency.DAILY.name)
        )
    }

    @Test
    fun `weekly advances seven days`() {
        assertEquals(
            LocalDate.of(2025, 3, 7),
            next(LocalDate.of(2025, 2, 28), RecurringFrequency.WEEKLY.name)
        )
    }

    @Test
    fun `biweekly advances fourteen days`() {
        assertEquals(
            LocalDate.of(2025, 3, 14),
            next(LocalDate.of(2025, 2, 28), RecurringFrequency.BIWEEKLY.name)
        )
    }

    @Test
    fun `yearly crosses leap year boundary`() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            next(LocalDate.of(2024, 2, 29), RecurringFrequency.YEARLY.name)
        )
    }

    @Test
    fun `unknown frequency falls back to monthly`() {
        // Same semantics as MONTHLY: Jan 31 + 1 month clamps to Feb 28
        assertEquals(
            LocalDate.of(2025, 2, 28),
            next(LocalDate.of(2025, 1, 31), "SOMETHING_ELSE")
        )
    }
}
