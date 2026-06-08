package com.aifactory.appmessagecapture.birthday.data

import androidx.room.TypeConverter

/**
 * Room 数据库类型转换器。
 *
 * 用于将 [ReminderType] 枚举与数据库可存储的 [String] 之间互相转换。
 */
class Converters {

    @TypeConverter
    fun fromReminderType(value: ReminderType): String {
        return value.name
    }

    @TypeConverter
    fun toReminderType(value: String): ReminderType {
        return ReminderType.fromName(value)
    }
}
