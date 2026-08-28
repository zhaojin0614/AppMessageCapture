package com.aifactory.appmessagecapture.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用图标全局 LruCache。
 *
 * 之前每个列表条目在主线程 remember 中同步 PackageManager IPC + 解码
 * 192×192 位图（实际显示仅 40dp 左右），滚动到新应用时掉帧。
 * 现在按 density 换算目标尺寸、IO 线程解码、全局共享缓存
 * （消息/账单列表中同一应用的图标高度重复）。
 */
object AppIconCache {
    // 64 个条目 × 约 40dp(≈120px)² ARGB ≈ 4.4MB 上限，实际远小于此
    private const val MAX_ENTRIES = 64
    private val cache = LruCache<String, Bitmap>(MAX_ENTRIES)

    // 标记“解码失败”（应用已卸载等），避免同一包名反复尝试
    private val FAILED = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    /** 缓存命中时同步返回，未命中或已知失败返回 null */
    fun getSync(packageName: String, sizePx: Int): ImageBitmap? =
        if (packageName.isBlank()) null
        else cache.get(key(packageName, sizePx))?.takeIf { it !== FAILED }?.asImageBitmap()

    /** IO 线程解码并入缓存 */
    suspend fun load(context: Context, packageName: String, sizePx: Int): ImageBitmap? =
        if (packageName.isBlank()) null   // 手动/周期记账无包名，不查 PackageManager
        else withContext(Dispatchers.IO) {
            val cached = cache.get(key(packageName, sizePx))
            when {
                cached === FAILED -> null
                cached != null -> cached.asImageBitmap()
                else -> {
                    val bitmap = try {
                        context.applicationContext.packageManager
                            .getApplicationIcon(packageName)
                            ?.toBitmap(width = sizePx, height = sizePx)
                    } catch (_: Exception) {
                        null
                    }
                    cache.put(key(packageName, sizePx), bitmap ?: FAILED)
                    bitmap?.asImageBitmap()
                }
            }
        }

    private fun key(packageName: String, sizePx: Int) = "$packageName@$sizePx"
}

/**
 * 按显示尺寸异步加载应用图标。
 * 首帧缓存未命中时返回 null（调用方渲染占位 UI），解码完成后自动重组刷新。
 */
@Composable
fun rememberAppIcon(packageName: String, sizeDp: Dp = 40.dp): State<ImageBitmap?> {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { sizeDp.roundToPx() }
    return produceState<ImageBitmap?>(
        initialValue = AppIconCache.getSync(packageName, sizePx),
        key1 = packageName,
        key2 = sizePx
    ) {
        // produceState 的 remember 状态不随 key 变化重置：列表槽位复用把一张卡片
        // 换成另一条账单/消息时，value 里残留的是上一个条目的图标，必须先按当前
        // 包名同步覆盖（命中即首帧正确；未命中先清空走占位），再异步加载。
        value = AppIconCache.getSync(packageName, sizePx)
        if (value == null) {
            value = AppIconCache.load(context, packageName, sizePx)
        }
    }
}
