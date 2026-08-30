package com.aifactory.appmessagecapture.birthday.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
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
 *
 * 尺寸策略：`SizeMode.LocalSize`——组件被拖拽缩放后按实际尺寸重新生成内容，
 * 紧凑档（高度 < 150dp，如 4x2/2x2）显示单行卡片，标准档显示双行卡片，
 * 列表用 LazyColumn 承载（行绘制严格裁切在组件边界内，缩小后不会再溢出）。
 */
object BirthdayWidget : GlanceAppWidget() {

    /** 数据上限：列表是 Lazy 的，实际可见条数由组件高度决定 */
    const val MAX_DISPLAY_COUNT = 20
    const val TAG = "BirthdayWidget"

    override val sizeMode: SizeMode = SizeMode.Exact

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
    val compact = LocalSize.current.height < 150.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<BirthdayWidgetActivity>())
            .padding(5.dp)
    ) {
        when (result) {
            is WidgetData.Success -> BirthdayWidgetContent(result.items, compact)
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
    items: List<Pair<BirthdayEntity, DateCalculator.BirthdayInfo>>,
    compact: Boolean
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // 标题行："生日管家"
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "生日管家",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_primary),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 12.sp else 14.sp
                )
            )
        }

        if (items.isEmpty()) {
            EmptyCard()
        } else {
            // Lazy 承载列表：行高超出组件高度时被严格裁切，不再溢出绘制
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                itemsIndexed(items) { _, (entity, info) ->
                    if (compact) {
                        BirthdayCompactRow(entity = entity, info = info)
                    } else {
                        BirthdayCard(entity = entity, info = info)
                    }
                }
                if (!compact) {
                    item {
                        FooterRow()
                    }
                }
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

/** 紧凑单行卡片：小尺寸（如 4x2）下可见条数更多 */
@Composable
private fun BirthdayCompactRow(
    entity: BirthdayEntity,
    info: DateCalculator.BirthdayInfo
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
            (if (entity.isLunar) " 农历" else "") +
            (if (info.ageTurning != null) " 满${info.ageTurning}岁" else "")

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(ColorProvider(R.color.widget_card))
            .cornerRadius(8.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(28.dp)
                .height(28.dp)
                .background(ColorProvider(badgeBgColor))
                .cornerRadius(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayText,
                style = TextStyle(
                    color = ColorProvider(badgeTextColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
        Spacer(modifier = GlanceModifier.width(7.dp))
        Text(
            text = entity.name,
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_primary),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = dateLabel,
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_secondary),
                fontSize = 9.sp
            )
        )
    }
}

/** 标准双行卡片：中等及以上尺寸使用 */
@Composable
private fun BirthdayCard(
    entity: BirthdayEntity,
    info: DateCalculator.BirthdayInfo
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
            .padding(bottom = 3.dp)
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

/** 标准档末尾的入口提示 */
@Composable
private fun FooterRow() {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "打开App查看全部 ›",
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_secondary),
                fontSize = 11.sp
            )
        )
    }
}
