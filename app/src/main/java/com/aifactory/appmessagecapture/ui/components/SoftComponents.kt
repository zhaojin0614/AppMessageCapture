package com.aifactory.appmessagecapture.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifactory.appmessagecapture.ui.theme.GradientBrandEnd
import com.aifactory.appmessagecapture.ui.theme.GradientBrandStart
import com.aifactory.appmessagecapture.ui.theme.MistBlue
import com.aifactory.appmessagecapture.ui.theme.SandGold
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Soft UI shared components — the single visual language used across
 * Messages / Bills / Report / Birthday screens.
 */

// ---------- Glass helpers ----------

@Composable
fun isDarkTheme(): Boolean = isSystemInDarkTheme()

/**
 * 玻璃渐变填充：由单一颜色生成"左亮 → 中实 → 右暗"的水平渐变，
 * 用于彩色按钮/卡片/图标块，替代纯实色背景，保留透明度透出底层光斑。
 */
@Composable
fun gradientBrush(
    color: Color,
    alpha: Float = 1f,
    highlight: Float = 0.30f,
    shade: Float = 0.10f
): Brush {
    val base = color.copy(alpha = alpha)
    return Brush.horizontalGradient(
        listOf(
            lerp(base, Color.White, highlight),
            base,
            lerp(base, Color.Black, shade)
        )
    )
}

/** Frosted-glass panel fill — a faint translucent white tint so the
 *  glass surface reads as a distinct pane against the background while
 *  the animated backdrop blobs still shine through (no solid white
 *  panels, just a hint of glass body). */
@Composable
fun glassFill(): Color {
    val alpha = if (isDarkTheme()) 0.04f else 0.10f
    return Color.White.copy(alpha = alpha)
}

/** Edge light for glass surfaces — a gradient rim (brightest at the
 *  top edge, fading down into the background) instead of a uniform
 *  outline, so surfaces blend into the backdrop like liquid glass
 *  instead of drawing a hard divider line. */
@Composable
fun glassBorder(): BorderStroke {
    val dark = isDarkTheme()
    return BorderStroke(
        1.dp,
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (dark) 0.22f else 0.45f),
            0.5f to Color.White.copy(alpha = if (dark) 0.12f else 0.25f),
            1f to Color.White.copy(alpha = if (dark) 0.07f else 0.15f)
        )
    )
}

/** Top-edge light reflection used inside glass surfaces: a bright
 *  catch-light band along the top edge that fades down into the pane,
 *  giving the liquid-glass rim its signature sheen. */
@Composable
fun glassHighlightBrush(): Brush {
    val dark = isDarkTheme()
    return Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (dark) 0.15f else 0.34f),
        0.10f to Color.White.copy(alpha = if (dark) 0.06f else 0.15f),
        1f to Color.Transparent,
        startY = 0f,
        endY = 240f
    )
}

// ---------- SoftCard: liquid-glass card ----------

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(glassFill())
            .border(glassBorder(), shape)
    ) {
        // Top light reflection (liquid glass highlight)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassHighlightBrush())
        )
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

// ---------- SoftGradientCard: gradient hero card (bento style) ----------

@Composable
fun SoftGradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    contentColor: Color = Color.White,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = brush.toAmbientColor(),
                spotColor = brush.toAmbientColor()
            )
            .clip(shape)
            .background(brush)
            .border(glassBorder(), shape)
    ) {
        // Top light reflection (liquid glass highlight)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassHighlightBrush())
        )
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

private fun Brush.toAmbientColor(): Color = when (this) {
    is androidx.compose.ui.graphics.SolidColor -> value.copy(alpha = 0.25f)
    else -> Color(0x1F000000)
}

// ---------- SoftButton: unified primary/secondary button ----------

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    height: Dp = 46.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(
                if (enabled) gradientBrush(backgroundColor, alpha = 0.88f)
                else gradientBrush(backgroundColor, alpha = 0.4f)
            )
            .border(glassBorder(), shape)
            .softClickable(
                onClick = onClick,
                enabled = enabled
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun Modifier.softClickable(
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier {
    val interactionSource = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

// ---------- PillToggle: segmented control ----------

/**
 * 分段控件：选中胶囊是独立「滑块」，用 drawBehind 画在底色之上、文字之下，
 * 切换时以弹簧动画在选项间滑动（与底部导航栏同款效果）。
 * 选项文字瞬时变色，几何一次布局到位，滑块只对最终位置做一次干净滑动。
 */
@Composable
fun PillToggle(
    options: List<Pair<String, Color>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    // 各选项几何（px，相对内容区原点）；首次测量直接落位，之后切换才滑动
    var itemLefts by remember { mutableStateOf(FloatArray(options.size)) }
    var itemWidths by remember { mutableStateOf(IntArray(options.size)) }
    var measured by remember { mutableStateOf(false) }
    val sliderLeft = remember { Animatable(0f) }
    val sliderWidth = remember { Animatable(0f) }
    val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)

    LaunchedEffect(itemLefts, itemWidths, safeIndex) {
        if (!measured) return@LaunchedEffect
        val targetLeft = itemLefts[safeIndex]
        val targetWidth = itemWidths[safeIndex].toFloat()
        if (sliderWidth.value == 0f) {
            sliderLeft.snapTo(targetLeft)
            sliderWidth.snapTo(targetWidth)
        } else {
            launch {
                sliderLeft.animateTo(
                    targetLeft,
                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                )
            }
            sliderWidth.animateTo(
                targetWidth,
                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
            )
        }
    }

    val selectedBrush = gradientBrush(options[safeIndex].second, alpha = 0.92f)

    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .border(glassBorder(), shape)
            .drawBehind {
                // 滑块画在底色之上、文字之下；坐标系含 3dp 内边距，需补回
                if (!measured || sliderWidth.value <= 0f) return@drawBehind
                val pad = 3.dp.toPx()
                drawRoundRect(
                    brush = selectedBrush,
                    topLeft = Offset(pad + sliderLeft.value, pad),
                    size = Size(sliderWidth.value, this.size.height - pad * 2),
                    cornerRadius = CornerRadius(11.dp.toPx())
                )
            }
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { index, (label, _) ->
            val selected = safeIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        val left = coords.positionInParent().x
                        val width = coords.size.width
                        if (itemLefts[index] != left || itemWidths[index] != width) {
                            val newLefts = itemLefts.copyOf(); newLefts[index] = left
                            val newWidths = itemWidths.copyOf(); newWidths[index] = width
                            itemLefts = newLefts
                            itemWidths = newWidths
                            measured = true
                        }
                    }
                    .clip(RoundedCornerShape(11.dp))
                    .softClickable(onClick = { onSelect(index) })
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 紧凑版玻璃弹窗：M3 AlertDialog 的按钮区自带上下约 24dp 固定留白且无法
 * 通过参数调小，这里改用玻璃容器 + 右对齐紧凑操作行统一弹窗观感。
 * [text] 承载任意内容（表单/列表均可），[title] 为空时不占标题区。
 */
@Composable
fun GlassCompactDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: String? = null,
    text: (@Composable () -> Unit)? = null,
    properties: androidx.compose.ui.window.DialogProperties =
        androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(
                alpha = if (isDarkTheme()) 0.90f else 0.93f
            ),
            modifier = modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .border(glassBorder(), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)) {
                if (title != null) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (text != null) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) { text() }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                // 取消 48dp 最小点击目标对按钮行的抬高，弹窗按钮本身已有 40dp 高度
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}

// ---------- SoftFab: floating circular action button ----------

/**
 * Floating circular action button — a plain glass circle with the icon
 * centered, nothing else. Replaces Material 3 FloatingActionButton whose
 * newer spec draws an extra inner tonal icon container (the octagon-like
 * shape in the middle of the button).
 */
@Composable
fun SoftFab(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = Color.White,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x2E000000)
            )
            .clip(CircleShape)
            .background(gradientBrush(backgroundColor, alpha = 0.88f))
            .border(glassBorder(), CircleShape)
            .softClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ---------- SoftEmptyState: unified empty view ----------

@Composable
fun SoftEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 88.dp
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize / 2.5f))
                .background(gradientBrush(MaterialTheme.colorScheme.primaryContainer, alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(iconSize * 0.45f)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- StatPill: small value+label pill for stats strips ----------

@Composable
fun StatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- AmbientBackground: living light blobs behind glass ----------

/**
 * Decorative ambient light blobs placed behind the translucent glass
 * surfaces so the frosted effect has color to refract.
 *
 * The background is alive: four blobs (mint / blue / gold / violet)
 * slowly drift, breathe (alpha pulses) and flow between high-saturation
 * cold-warm color pairs, with deliberately different cycle durations so
 * the motion stays organic. Because the blobs live under the translucent
 * glass, the liquid-glass surfaces pick up the motion — a true dynamic
 * backdrop.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier
) {
    val dark = isDarkTheme()
    val transition = rememberInfiniteTransition(label = "ambientBg")

    // 波光轨迹：x/y 用不同频率的三角波经正弦变换 → Lissajous 曲线，幅度大，
    // 光斑可以在整个屏幕范围内游走
    val t1x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "t1x")
    val t1y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(9500, easing = LinearEasing), RepeatMode.Reverse), label = "t1y")
    val t2x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "t2x")
    val t2y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "t2y")
    val t3x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse), label = "t3x")
    val t3y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(14500, easing = LinearEasing), RepeatMode.Reverse), label = "t3y")
    val t4x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "t4x")
    val t4y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "t4y")

    // 颜色缓慢流动：高饱和冷暖交替（薄荷↔暖金 / 雾蓝↔紫罗兰 / 暖金↔珊瑚 / 亮紫↔青 / 草绿↔金黄 / 粉↔蓝）
    val c1 by transition.animateColor(GradientBrandStart, SandGold, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "c1")
    val c2 by transition.animateColor(MistBlue, Color(0xFFA97BD6), infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Reverse), label = "c2")
    val c3 by transition.animateColor(SandGold, Color(0xFFEE7B6C), infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Reverse), label = "c3")
    val c4 by transition.animateColor(Color(0xFF9B8CE8), Color(0xFF4CB5C0), infiniteRepeatable(tween(32000, easing = LinearEasing), RepeatMode.Reverse), label = "c4")
    val c5 by transition.animateColor(Color(0xFF3FAE7E), Color(0xFFF2B84B), infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "c5")
    val c6 by transition.animateColor(Color(0xFFEE7BA6), Color(0xFF5B9BD8), infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse), label = "c6")

    // 中部光斑轨迹（填补屏幕中间的空缺）
    val t5x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "t5x")
    val t5y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(11500, easing = LinearEasing), RepeatMode.Reverse), label = "t5y")
    val t6x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "t6x")
    val t6y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(13500, easing = LinearEasing), RepeatMode.Reverse), label = "t6y")

    // 光斑 7/8/9 轨迹：中上 / 左中 / 右中，填补屏幕边缘与中部之间的空隙
    val t7x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(16500, easing = LinearEasing), RepeatMode.Reverse), label = "t7x")
    val t7y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "t7y")
    val t8x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(19500, easing = LinearEasing), RepeatMode.Reverse), label = "t8x")
    val t8y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "t8y")
    val t9x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(15500, easing = LinearEasing), RepeatMode.Reverse), label = "t9x")
    val t9y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(17500, easing = LinearEasing), RepeatMode.Reverse), label = "t9y")

    // 光斑 7/8/9 颜色：金黄↔青 / 珊瑚↔薄荷 / 紫罗兰↔暖金
    val c7 by transition.animateColor(Color(0xFFF2B84B), Color(0xFF4CB5C0), infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse), label = "c7")
    val c8 by transition.animateColor(Color(0xFFEE7B6C), Color(0xFF3FAE7E), infiniteRepeatable(tween(21000, easing = LinearEasing), RepeatMode.Reverse), label = "c8")
    val c9 by transition.animateColor(Color(0xFFA97BD6), Color(0xFFF2B84B), infiniteRepeatable(tween(27000, easing = LinearEasing), RepeatMode.Reverse), label = "c9")

    // 极光渐变底色：四段色带独立往复流动（周期错相不同步），整屏色调持续漂移，
    // 玻璃面板后面是"全屏在动"的柔和色彩，而非只有光斑在移动
    val bg1 by transition.animateColor(
        if (dark) Color(0xFF12231E) else Color(0xFFBFEBDD),
        if (dark) Color(0xFF1B1A2E) else Color(0xFFC2C4F7),
        infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg1"
    )
    val bg2 by transition.animateColor(
        if (dark) Color(0xFF16222F) else Color(0xFFD8E6F8),
        if (dark) Color(0xFF241A28) else Color(0xFFF4DCE9),
        infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg2"
    )
    val bg3 by transition.animateColor(
        if (dark) Color(0xFF221C15) else Color(0xFFF6E9D4),
        if (dark) Color(0xFF132423) else Color(0xFFD4F0EA),
        infiniteRepeatable(tween(13500, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg3"
    )
    val bg4 by transition.animateColor(
        if (dark) Color(0xFF1E1730) else Color(0xFFEADFF5),
        if (dark) Color(0xFF122622) else Color(0xFFCDEFE6),
        infiniteRepeatable(tween(10500, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg4"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 全屏流动的极光渐变底色（最底层）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to bg1,
                        0.33f to bg2,
                        0.67f to bg3,
                        1f to bg4
                    )
                )
        )
        // Mint blob — top-left（薄荷 ↔ 暖金），大范围游走
        // 光斑位移用 graphicsLayer 平移（draw 阶段读取动画值）而非 Modifier.offset，
        // 避免 9 个光斑每帧触发布局传递，视觉效果完全一致
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .graphicsLayer {
                    translationX = (-80 + sin(t1x * PI.toFloat()) * 90).dp.toPx()
                    translationY = (-110 + sin(t1y * PI.toFloat()) * 85).dp.toPx()
                }
                .size(470.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c1.copy(alpha = if (dark) 0.48f else 0.66f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Blue blob — top-right（雾蓝 ↔ 紫罗兰），大范围游走
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer {
                    translationX = (90 - sin(t2x * PI.toFloat()) * 90).dp.toPx()
                    translationY = (40 + sin(t2y * PI.toFloat()) * 80).dp.toPx()
                }
                .size(430.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c2.copy(alpha = if (dark) 0.44f else 0.60f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Gold blob — bottom-left（暖金 ↔ 珊瑚红），大范围游走
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer {
                    translationX = (-70 + sin(t3x * PI.toFloat()) * 95).dp.toPx()
                    translationY = (70 + sin(t3y * PI.toFloat()) * 85).dp.toPx()
                }
                .size(450.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c3.copy(alpha = if (dark) 0.40f else 0.54f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Violet blob — bottom-right（亮紫 ↔ 青），大范围游走
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = (-90 - sin(t4x * PI.toFloat()) * 85).dp.toPx()
                    translationY = (-50 + sin(t4y * PI.toFloat()) * 80).dp.toPx()
                }
                .size(410.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c4.copy(alpha = if (dark) 0.36f else 0.48f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Center blob — 屏幕正中（草绿 ↔ 金黄），填补中间空缺
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = (sin(t5x * PI.toFloat()) * 55).dp.toPx()
                    translationY = (sin(t5y * PI.toFloat()) * 50).dp.toPx()
                }
                .size(390.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c5.copy(alpha = if (dark) 0.36f else 0.48f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Lower-middle blob — 中下部（粉 ↔ 蓝），填补下方中部空缺
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = (sin(t6x * PI.toFloat()) * 60).dp.toPx()
                    translationY = (100 + sin(t6y * PI.toFloat()) * 55).dp.toPx()
                }
                .size(370.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c6.copy(alpha = if (dark) 0.32f else 0.44f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Upper-middle blob — 中上（金黄 ↔ 青），填补顶部中部空缺
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = (sin(t7x * PI.toFloat()) * 65).dp.toPx()
                    translationY = (-150 + sin(t7y * PI.toFloat()) * 55).dp.toPx()
                }
                .size(260.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c7.copy(alpha = if (dark) 0.26f else 0.38f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Left-middle blob — 左中（珊瑚 ↔ 薄荷），填补左侧中部空缺
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = (-70 + sin(t8x * PI.toFloat()) * 65).dp.toPx()
                    translationY = (sin(t8y * PI.toFloat()) * 90).dp.toPx()
                }
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c8.copy(alpha = if (dark) 0.24f else 0.36f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Right-middle blob — 右中（紫罗兰 ↔ 暖金），填补右侧中部空缺
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = (70 - sin(t9x * PI.toFloat()) * 65).dp.toPx()
                    translationY = (sin(t9y * PI.toFloat()) * 90).dp.toPx()
                }
                .size(300.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            c9.copy(alpha = if (dark) 0.22f else 0.34f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
