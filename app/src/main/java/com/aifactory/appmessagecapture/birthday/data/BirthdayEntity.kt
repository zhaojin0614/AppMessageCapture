package com.aifactory.appmessagecapture.birthday.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 生日记录实体类。
 *
 * @property id 主键，自增
 * @property name 亲友姓名
 * @property isLunar 是否为农历生日（true=农历，false=公历）
 * @property birthYear 出生年份，可为 null（未知年份则无法计算将满岁数）
 * @property birthMonth 出生月份（农历 1-12，公历 1-12）
 * @property birthDay 出生日期（农历 1-30，公历 1-31）
 * @property reminderType 提醒类型，见 [ReminderType]
 * @property reminderTime 提醒时间，格式 "HH:mm"（如 "08:30"），null 表示使用默认时间
 */
@Entity(
    tableName = "birthdays",
    indices = [Index(value = ["name"])]
)
data class BirthdayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isLunar: Boolean,
    val birthYear: Int?,
    val birthMonth: Int,
    val birthDay: Int,
    val reminderType: ReminderType = ReminderType.ON_DAY,
    val reminderTime: String? = null
)
