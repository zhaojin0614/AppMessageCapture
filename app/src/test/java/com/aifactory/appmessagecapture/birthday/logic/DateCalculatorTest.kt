package com.aifactory.appmessagecapture.birthday.logic

import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Tests for [DateCalculator] and [LunarCalendarAdapter] (production code).
 * baseDate is injected so results are deterministic.
 */
class DateCalculatorTest {

    private fun calendarOf(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 0, 0, 0)
        }

    // ── Solar birthday ────────────────────────────────────────────────────

    @Test
    fun `solar birthday today gives zero days left`() {
        val birthday = BirthdayEntity(name = "小明", isLunar = false, birthYear = 2000, birthMonth = 6, birthDay = 15)
        val info = DateCalculator.calculate(birthday, calendarOf(2026, 6, 15))
        assertEquals(0, info.daysLeft)
        assertEquals(2026, info.nextSolarYear)
        assertEquals(26, info.ageTurning)
    }

    @Test
    fun `solar birthday tomorrow gives one day left`() {
        val birthday = BirthdayEntity(name = "小明", isLunar = false, birthYear = null, birthMonth = 6, birthDay = 16)
        val info = DateCalculator.calculate(birthday, calendarOf(2026, 6, 15))
        assertEquals(1, info.daysLeft)
        assertNull(info.ageTurning) // unknown birth year
    }

    @Test
    fun `solar birthday already passed rolls to next year`() {
        val birthday = BirthdayEntity(name = "小明", isLunar = false, birthYear = 1990, birthMonth = 6, birthDay = 15)
        val info = DateCalculator.calculate(birthday, calendarOf(2026, 6, 16))
        assertEquals(2027, info.nextSolarYear)
        assertEquals(364, info.daysLeft) // 2027-06-15 minus 2026-06-16
        assertEquals(37, info.ageTurning) // 2027 - 1990
    }

    @Test
    fun `solar birthday across year boundary`() {
        // Birthday Jan 1, today Dec 31 2026 -> next birthday Jan 1 2027, 1 day left
        val birthday = BirthdayEntity(name = "小明", isLunar = false, birthYear = null, birthMonth = 1, birthDay = 1)
        val info = DateCalculator.calculate(birthday, calendarOf(2026, 12, 31))
        assertEquals(2027, info.nextSolarYear)
        assertEquals(1, info.daysLeft)
    }

    @Test
    fun `base date with time component still counts whole days`() {
        // 10:00 on the same date must not shift the day count
        val birthday = BirthdayEntity(name = "小明", isLunar = false, birthYear = null, birthMonth = 6, birthDay = 16)
        val base = Calendar.getInstance().apply {
            clear()
            set(2026, 5, 15, 10, 30, 0)
        }
        val info = DateCalculator.calculate(birthday, base)
        assertEquals(1, info.daysLeft)
    }

    // ── Lunar birthday ────────────────────────────────────────────────────

    @Test
    fun `lunar to solar conversion for spring festival`() {
        // Chinese New Year 2025 (lunar 2025-1-1) fell on 2025-01-29
        val solar = LunarCalendarAdapter.lunarToSolar(2025, 1, 1)
        assertEquals(Triple(2025, 1, 29), solar)
    }

    @Test
    fun `solar to lunar roundtrip for spring festival 2026`() {
        // Chinese New Year 2026 fell on 2026-02-17
        val lunar = LunarCalendarAdapter.solarToLunar(2026, 2, 17)
        assertEquals(Triple(2026, 1, 1), lunar)
        val roundtrip = LunarCalendarAdapter.lunarToSolar(lunar.first, lunar.second, lunar.third)
        assertEquals(Triple(2026, 2, 17), roundtrip)
    }

    @Test
    fun `lunar birthday not yet passed stays in current lunar year`() {
        // Lunar 2026-1-1 = 2026-02-17; base 2026-01-15 is before it
        val birthday = BirthdayEntity(name = "小红", isLunar = true, birthYear = 1995, birthMonth = 1, birthDay = 1)
        val info = DateCalculator.calculate(birthday, calendarOf(2026, 1, 15))
        assertEquals("2026-02-17", info.nextSolarDateString())
        assertEquals(33, info.daysLeft) // 2026-01-15 -> 2026-02-17
    }

    @Test
    fun `calculateForYear maps lunar birthday to its solar date`() {
        val birthday = BirthdayEntity(name = "小红", isLunar = true, birthYear = null, birthMonth = 1, birthDay = 1)
        val (y, m, d) = DateCalculator.calculateForYear(2025, birthday)
        assertEquals(Triple(2025, 1, 29), Triple(y, m, d))
    }

    @Test
    fun `calculateAll sorts by daysLeft ascending`() {
        val later = BirthdayEntity(name = "晚", isLunar = false, birthYear = null, birthMonth = 6, birthDay = 20)
        val sooner = BirthdayEntity(name = "早", isLunar = false, birthYear = null, birthMonth = 6, birthDay = 16)
        val sorted = DateCalculator.calculateAll(listOf(later, sooner), calendarOf(2026, 6, 15))
        assertEquals("早", sorted.first().first.name)
        assertEquals("晚", sorted.last().first.name)
        assertNotNull(sorted.first().second.daysLeft)
    }
}
