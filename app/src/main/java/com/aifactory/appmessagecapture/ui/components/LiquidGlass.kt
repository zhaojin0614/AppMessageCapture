package com.aifactory.appmessagecapture.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize

/**
 * 真·动态背景模糊容器（iOS 毛玻璃效果）。
 *
 * 原理：把子树内容先绘制到离屏层（GraphicsLayer），用 BlurEffect 模糊后
 * 作为最底层垫底；随后再正常绘制一遍内容。这样：
 * - 不透明内容（文字、图标、实色卡片）覆盖在模糊层上，保持清晰；
 * - 透明组件（glassFill 面板、玻璃卡片）透出底下的模糊层，
 *   滚动时模糊内容实时跟随 —— 与 iOS 的 backdrop blur 观感一致。
 *
 * 仅 API 31+（Android 12+）支持 RenderEffect 模糊；低版本自动退化为
 * 普通单次绘制（组件仍为纯透明 + 高光描边）。
 *
 * @param blurRadius 模糊半径，越大玻璃感越强（也越费 GPU）
 */
@Composable
fun GlassBackdropRoot(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    val radiusPx = with(LocalDensity.current) { blurRadius.toPx() }
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurEffect = remember(radiusPx, canBlur) {
        if (canBlur) BlurEffect(radiusPx, radiusPx, TileMode.Mirror) else null
    }

    Box(
        modifier = modifier.drawWithContent {
            if (blurEffect != null) {
                // 1) 离屏捕获整棵子树（玻璃组件同样被捕获）
                graphicsLayer.renderEffect = blurEffect
                graphicsLayer.record(
                    size = this.size.toIntSize(),
                    density = this,
                    layoutDirection = this.layoutDirection
                ) {
                    this@drawWithContent.drawContent()
                }
                // 2) 模糊副本垫底
                drawLayer(graphicsLayer)
                // 3) 正常内容覆盖 —— 透明玻璃区域透出模糊，其余保持清晰
                drawContent()
            } else {
                drawContent()
            }
        }
    ) {
        content()
    }
}
