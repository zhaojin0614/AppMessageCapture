package com.aifactory.appmessagecapture.birthday.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.logic.DateCalculator
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.data.AppDatabase
import kotlinx.coroutines.CancellationException

/**
 * 桌面组件 Widget（单例）。
 *
 * 使用 `object` 确保 Glance 框架始终操作同一个实例，避免 session 管理混乱。
 * `provideGlance` 中不阻塞、不 delay，尽快完成数据加载并调用 `provideContent`。
 */
object BirthdayWidget : GlanceAppWidget() {

    const val MAX_DISPLAY_COUNT = 5
    const val TAG = "BirthdayWidget"

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        BirthdayLog.d("[$TAG] provideGlance start. glanceId=$id")

        val result: WidgetData = try {
            val dao = AppDatabase.getDatabase(context).birthdayDao()
            // 使用 getAllOnce() 直接查询，避免 Flow 订阅/发射的竞态问题
            val entities = dao.getAllOnce()
            val calculated = DateCalculator.calculateAll(entities)
            val topItems = calculated.take(MAX_DISPLAY_COUNT)
            BirthdayLog.i(
                "[$TAG] Loaded %d records, displaying top %d.",
                entities.size,
                topItems.size
            )
            WidgetData.Success(topItems)
        } catch (e: CancellationException) {
            // 必须重新抛出 CancellationException，否则 Glance session 无法正常取消/重建
            BirthdayLog.d("[$TAG] provideGlance cancelled. glanceId=$id")
            throw e
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.provideGlance", e)
            WidgetData.Error(e.message ?: "Unknown error")
        }

        provideContent {
            BirthdayWidgetRoot(result)
        }
    }

    /**
     * 刷新所有已放置的桌面小组件实例。
     *
     * 逐个调用 [update] 以兼容 Glance 1.1.1（该版本没有 [updateAll] 扩展函数）。
     */
    suspend fun updateAll(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(BirthdayWidget::class.java)
            if (glanceIds.isNotEmpty()) {
                glanceIds.forEach { glanceId ->
                    try {
                        update(context, glanceId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        BirthdayLog.logException("[BirthdayWidget] updateAll $glanceId", e)
                    }
                }
            }
        } catch (e: Exception) {
            BirthdayLog.logException("[BirthdayWidget] updateAll", e)
        }
    }
}

// 文件级别的 sealed class，供本文件内所有 Composable 使用
private sealed class WidgetData {
    data class Success(val items: List<Pair<BirthdayEntity, DateCalculator.BirthdayInfo>>) : WidgetData()
    data class Error(val message: String) : WidgetData()
}

@Composable
private fun BirthdayWidgetRoot(result: WidgetData) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<BirthdayWidgetActivity>())
            .padding(5.dp)
    ) {
        when (result) {
            is WidgetData.Success -> BirthdayWidgetContent(result.items)
            is WidgetData.Error -> WidgetErrorContent(result.message)
        }
    }
}

@Composable
private fun WidgetErrorContent(message: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Widget Error:\n$message",
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_primary),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun BirthdayWidgetContent(
    items: List<Pair<BirthdayEntity, DateCalculator.BirthdayInfo>>
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // 标题行：橙色圆角图标 + "生日管家"
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "生日管家",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_primary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
        }

        if (items.isEmpty()) {
            EmptyCard()
        } else {
            items.forEachIndexed { index, (entity, info) ->
                val isLast = index == items.lastIndex
                BirthdayCard(entity = entity, info = info, isLast = isLast)
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(R.color.widget_card))
            .cornerRadius(10.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(32.dp)
                .height(32.dp)
                .background(ColorProvider(R.color.widget_secondary_light))
                .cornerRadius(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_secondary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "暂无生日记录",
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_secondary),
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun BirthdayCard(
    entity: BirthdayEntity,
    info: DateCalculator.BirthdayInfo,
    isLast: Boolean
) {
    val isToday = info.daysLeft == 0
    val isSoon = info.daysLeft in 1..7
    val dayText = if (isToday) "今天" else "${info.daysLeft}天"

    val badgeBgColor = when {
        isToday -> R.color.widget_primary
        isSoon -> R.color.widget_primary_light
        else -> R.color.widget_secondary_light
    }
    val badgeTextColor = when {
        isToday -> R.color.widget_text_white
        isSoon -> R.color.widget_primary_dark
        else -> R.color.widget_secondary
    }

    val dateLabel = info.nextSolarDateString() +
            (if (entity.isLunar) " (农历)" else "") +
            (if (info.ageTurning != null) " · 满${info.ageTurning}岁" else "")

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(R.color.widget_card))
            .cornerRadius(10.dp)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 天数标签
        Box(
            modifier = GlanceModifier
                .width(36.dp)
                .height(36.dp)
                .background(ColorProvider(badgeBgColor))
                .cornerRadius(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayText,
                style = TextStyle(
                    color = ColorProvider(badgeTextColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = entity.name,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_primary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = dateLabel,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_secondary),
                    fontSize = 11.sp
                )
            )
        }
    }
}
