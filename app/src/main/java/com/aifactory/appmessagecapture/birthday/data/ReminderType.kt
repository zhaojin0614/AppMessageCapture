package com.aifactory.appmessagecapture.birthday.data

/**
 * 生日提醒类型枚举。
 *
 * 定义了在亲友生日到来前，何时触发提醒。
 */
enum class ReminderType(val displayName: String, val daysBefore: Int) {
    NONE("不提醒", 0),
    ON_DAY("当天提醒", 0),
    ONE_DAY_BEFORE("提前 1 天", 1),
    THREE_DAYS_BEFORE("提前 3 天", 3),
    ONE_WEEK_BEFORE("提前 1 周", 7);

    companion object {
        /**
         * 根据枚举名称解析，若解析失败默认返回 [ON_DAY]。
         */
        fun fromName(name: String): ReminderType {
            return entries.find { it.name == name } ?: ON_DAY
        }
    }
}
