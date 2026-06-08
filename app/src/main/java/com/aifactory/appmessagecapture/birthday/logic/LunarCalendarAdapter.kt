package com.aifactory.appmessagecapture.birthday.logic

import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar

/**
 * 农历库适配器。
 *
 * 封装 `cn.6tail:lunar` 库的所有调用，统一处理异常并打印详细日志。
 * 若后续替换农历库，仅需修改此文件。
 */
object LunarCalendarAdapter {

    private const val TAG = "LunarCalendarAdapter"

    /**
     * 将**公历**日期转换为农历日期。
     *
     * @return Triple(lunarYear, lunarMonth, lunarDay)
     */
    fun solarToLunar(solarYear: Int, solarMonth: Int, solarDay: Int): Triple<Int, Int, Int> {
        BirthdayLog.logMethodCall(
            "$TAG.solarToLunar",
            mapOf("solarYear" to solarYear, "solarMonth" to solarMonth, "solarDay" to solarDay)
        )
        return try {
            val solar = Solar(solarYear, solarMonth, solarDay)
            val lunar = solar.lunar
            val result = Triple(lunar.year, lunar.month, lunar.day)
            BirthdayLog.logMethodCall(
                "$TAG.solarToLunar",
                mapOf("solarYear" to solarYear, "solarMonth" to solarMonth, "solarDay" to solarDay),
                result
            )
            result
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.solarToLunar", e)
            throw e
        }
    }

    /**
     * 将**农历**月日转换为指定年份的**公历**日期。
     *
     * @param year 目标公历年份（例如 2026）
     * @param month 农历月份（1-12）
     * @param day 农历日期（1-30）
     * @return Triple(solarYear, solarMonth, solarDay)
     */
    fun lunarToSolar(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        BirthdayLog.logMethodCall(
            "$TAG.lunarToSolar",
            mapOf("year" to year, "lunarMonth" to month, "lunarDay" to day)
        )
        return try {
            val lunar = Lunar(year, month, day)
            val solar = lunar.solar
            val result = Triple(solar.year, solar.month, solar.day)
            BirthdayLog.logMethodCall(
                "$TAG.lunarToSolar",
                mapOf("year" to year, "lunarMonth" to month, "lunarDay" to day),
                result
            )
            result
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.lunarToSolar", e)
            throw e
        }
    }

    /**
     * 判断某公历日期对应的农历是否为闰月。
     */
    fun isLeapMonth(solarYear: Int, solarMonth: Int, solarDay: Int): Boolean {
        return try {
            Solar(solarYear, solarMonth, solarDay).lunar.month < 0
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.isLeapMonth", e)
            false
        }
    }
}
