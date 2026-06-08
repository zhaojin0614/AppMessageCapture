package com.aifactory.appmessagecapture.birthday.logic

import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import java.util.Calendar

/**
 * 生日日期计算工具类。
 *
 * 负责将 [BirthdayEntity] 转换为“下一次过生日的公历日期”，并计算：
 * - 倒数天数（daysLeft）
 * - 将满岁数（ageTurning，若 birthYear 已知）
 *
 * 所有计算入口均打印详细入参和结果日志，方便排障。
 */
object DateCalculator {

    private const val TAG = "DateCalculator"

    /**
     * 计算结果数据类。
     */
    data class BirthdayInfo(
        /** 下一次生日对应的公历年 */
        val nextSolarYear: Int,
        /** 下一次生日对应的公历月（1-12） */
        val nextSolarMonth: Int,
        /** 下一次生日对应的公历日（1-31） */
        val nextSolarDay: Int,
        /** 距离今天还剩多少天（0 表示今天就是生日） */
        val daysLeft: Int,
        /** 将满岁数（若 birthYear 未知则为 null） */
        val ageTurning: Int?
    ) {
        /**
         * 格式化下一次公历日期，例如 "2026-06-15"。
         */
        fun nextSolarDateString(): String =
            String.format("%04d-%02d-%02d", nextSolarYear, nextSolarMonth, nextSolarDay)
    }

    /**
     * 计算给定 [birthday] 的完整信息。
     *
     * @param birthday 生日实体
     * @param baseDate 基准日期，默认为当前系统时间。可注入用于单元测试。
     */
    fun calculate(
        birthday: BirthdayEntity,
        baseDate: Calendar = Calendar.getInstance()
    ): BirthdayInfo {
        BirthdayLog.logMethodCall(
            "$TAG.calculate",
            mapOf(
                "id" to birthday.id,
                "name" to birthday.name,
                "isLunar" to birthday.isLunar,
                "birthYear" to birthday.birthYear,
                "birthMonth" to birthday.birthMonth,
                "birthDay" to birthday.birthDay,
                "baseDate" to formatCalendar(baseDate)
            )
        )

        val today = stripTime(baseDate)
        val currentYear = today.get(Calendar.YEAR)

        val result = try {
            val (nextYear, nextMonth, nextDay) = if (birthday.isLunar) {
                findNextLunarBirthday(birthday, today, currentYear)
            } else {
                findNextSolarBirthday(birthday, today, currentYear)
            }

            val nextBirthdayCal = Calendar.getInstance().apply {
                set(nextYear, nextMonth - 1, nextDay, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = nextBirthdayCal.timeInMillis - today.timeInMillis
            val daysLeft = (diffMillis / (24 * 60 * 60 * 1000L)).toInt()

            val ageTurning = if (birthday.birthYear != null && birthday.birthYear > 0) {
                nextYear - birthday.birthYear
            } else null

            BirthdayInfo(
                nextSolarYear = nextYear,
                nextSolarMonth = nextMonth,
                nextSolarDay = nextDay,
                daysLeft = daysLeft,
                ageTurning = ageTurning
            )
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.calculate", e)
            // 异常降级：返回一个“很远”的日期，避免 UI 崩溃
            BirthdayInfo(currentYear, 1, 1, Int.MAX_VALUE, null)
        }

        BirthdayLog.logMethodCall(
            "$TAG.calculate",
            mapOf("name" to birthday.name),
            result
        )
        return result
    }

    /**
     * 批量计算，返回按 [BirthdayInfo.daysLeft] 升序排列的结果。
     */
    fun calculateAll(
        birthdays: List<BirthdayEntity>,
        baseDate: Calendar = Calendar.getInstance()
    ): List<Pair<BirthdayEntity, BirthdayInfo>> {
        BirthdayLog.d("[$TAG] calculateAll called with %d records", birthdays.size)
        val list = birthdays.map { it to calculate(it, baseDate) }
        val sorted = list.sortedBy { it.second.daysLeft }
        BirthdayLog.d("[$TAG] calculateAll completed. Sorted %d records by daysLeft.", sorted.size)
        return sorted
    }

    // -------------------------------------------------------------------------
    // 私有实现：公历生日
    // -------------------------------------------------------------------------

    private fun findNextSolarBirthday(
        birthday: BirthdayEntity,
        today: Calendar,
        currentYear: Int
    ): Triple<Int, Int, Int> {
        BirthdayLog.d(
            "[$TAG.findNextSolarBirthday] name=%s, currentYear=%d, today=%s",
            birthday.name, currentYear, formatCalendar(today)
        )

        val thisYearBirthday = Calendar.getInstance().apply {
            set(currentYear, birthday.birthMonth - 1, birthday.birthDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        BirthdayLog.d(
            "[$TAG.findNextSolarBirthday] thisYearBirthday=%s, isBeforeToday=%s",
            formatCalendar(thisYearBirthday),
            thisYearBirthday.timeInMillis < today.timeInMillis
        )

        return if (thisYearBirthday.timeInMillis < today.timeInMillis) {
            // 今年生日已过，找下一年
            Triple(currentYear + 1, birthday.birthMonth, birthday.birthDay)
        } else {
            Triple(currentYear, birthday.birthMonth, birthday.birthDay)
        }
    }

    // -------------------------------------------------------------------------
    // 私有实现：农历生日
    // -------------------------------------------------------------------------

    private fun findNextLunarBirthday(
        birthday: BirthdayEntity,
        today: Calendar,
        currentYear: Int
    ): Triple<Int, Int, Int> {
        BirthdayLog.d(
            "[$TAG.findNextLunarBirthday] name=%s, currentYear=%d, today=%s",
            birthday.name, currentYear, formatCalendar(today)
        )

        // 1. 尝试当前年份对应的公历日期
        val thisYearSolar = LunarCalendarAdapter.lunarToSolar(
            currentYear, birthday.birthMonth, birthday.birthDay
        )
        BirthdayLog.d(
            "[$TAG.findNextLunarBirthday] %s lunar(%d-%d-%d) -> solar thisYear=%s",
            birthday.name, currentYear, birthday.birthMonth, birthday.birthDay, thisYearSolar
        )

        val thisYearCal = Calendar.getInstance().apply {
            set(thisYearSolar.first, thisYearSolar.second - 1, thisYearSolar.third, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return if (thisYearCal.timeInMillis < today.timeInMillis) {
            // 今年农历生日已过，计算下一年
            val nextYearSolar = LunarCalendarAdapter.lunarToSolar(
                currentYear + 1, birthday.birthMonth, birthday.birthDay
            )
            BirthdayLog.d(
                "[$TAG.findNextLunarBirthday] %s thisYear passed, nextYearSolar=%s",
                birthday.name, nextYearSolar
            )
            nextYearSolar
        } else {
            thisYearSolar
        }
    }

    // -------------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------------

    /**
     * 去除 Calendar 中的时分秒毫秒，仅保留日期部分。
     */
    private fun stripTime(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun formatCalendar(cal: Calendar): String {
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
