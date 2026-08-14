package com.aifactory.appmessagecapture.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.ui.theme.GradientBrandEnd
import com.aifactory.appmessagecapture.ui.theme.GradientBrandStart
import com.aifactory.appmessagecapture.ui.theme.MistBlue
import com.aifactory.appmessagecapture.ui.theme.SandGold
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

@Composable
fun PillToggle(
    options: List<Pair<String, Color>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .border(glassBorder(), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { index, (label, color) ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (selected) gradientBrush(color, alpha = 0.92f)
                        else SolidColor(Color.Transparent)
                    )
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

// ---------- GlassAlertDialog: transparent dialog with glass rim ----------

/**
 * AlertDialog with a frosted glass container + highlight border: a
 * near-opaque surface keeps content readable (fully transparent dialogs
 * were unreadable), while the glass rim keeps the liquid-glass language.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    iconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties()
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.border(glassBorder(), shape),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (isDarkTheme()) 0.90f else 0.93f
        ),
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = 0.dp,
        properties = properties
    )
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

// ---------- SectionHeader ----------

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        if (trailing != null) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                trailing()
            }
        }
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

// ---------- AppIconBox: rounded icon container with fallback ----------

@Composable
fun AppIconBox(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    icon: ImageVector? = null,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradientBrush(backgroundColor, alpha = 0.75f))
            .border(glassBorder(), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.5f)
            )
        }
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

// ---------- GradientHeader: signature mint-teal screen header ----------

@Composable
fun GradientHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
    brush: Brush = Brush.linearGradient(listOf(GradientBrandStart, GradientBrandEnd))
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
            )
            .background(brush)
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
        }
        if (bottomContent != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                content = bottomContent
            )
        }
    }
}

// ---------- StatsCardRow: floating white stats card ----------

@Composable
fun StatsCardRow(
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    valueColors: List<Color> = emptyList()
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stats.forEachIndexed { index, (value, label) ->
                StatPill(
                    value = value,
                    label = label,
                    valueColor = if (index < valueColors.size) valueColors[index]
                    else MaterialTheme.colorScheme.onBackground
                )
            }
        }
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
    val t1x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(8500, easing = LinearEasing), RepeatMode.Reverse), label = "t1x")
    val t1y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(6200, easing = LinearEasing), RepeatMode.Reverse), label = "t1y")
    val t2x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(10500, easing = LinearEasing), RepeatMode.Reverse), label = "t2x")
    val t2y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(7800, easing = LinearEasing), RepeatMode.Reverse), label = "t2y")
    val t3x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(12500, easing = LinearEasing), RepeatMode.Reverse), label = "t3x")
    val t3y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(9500, easing = LinearEasing), RepeatMode.Reverse), label = "t3y")
    val t4x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(14500, easing = LinearEasing), RepeatMode.Reverse), label = "t4x")
    val t4y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "t4y")

    // 颜色缓慢流动：高饱和冷暖交替（薄荷↔暖金 / 雾蓝↔紫罗兰 / 暖金↔珊瑚 / 亮紫↔青 / 草绿↔金黄 / 粉↔蓝）
    val c1 by transition.animateColor(GradientBrandStart, SandGold, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "c1")
    val c2 by transition.animateColor(MistBlue, Color(0xFFA97BD6), infiniteRepeatable(tween(16500, easing = LinearEasing), RepeatMode.Reverse), label = "c2")
    val c3 by transition.animateColor(SandGold, Color(0xFFEE7B6C), infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse), label = "c3")
    val c4 by transition.animateColor(Color(0xFF9B8CE8), Color(0xFF4CB5C0), infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "c4")
    val c5 by transition.animateColor(Color(0xFF3FAE7E), Color(0xFFF2B84B), infiniteRepeatable(tween(14500, easing = LinearEasing), RepeatMode.Reverse), label = "c5")
    val c6 by transition.animateColor(Color(0xFFEE7BA6), Color(0xFF5B9BD8), infiniteRepeatable(tween(17500, easing = LinearEasing), RepeatMode.Reverse), label = "c6")

    // 中部光斑轨迹（填补屏幕中间的空缺）
    val t5x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(9800, easing = LinearEasing), RepeatMode.Reverse), label = "t5x")
    val t5y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Reverse), label = "t5y")
    val t6x by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(11800, easing = LinearEasing), RepeatMode.Reverse), label = "t6x")
    val t6y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(8800, easing = LinearEasing), RepeatMode.Reverse), label = "t6y")

    Box(modifier = modifier.fillMaxSize()) {
        // Mint blob — top-left（薄荷 ↔ 暖金），大范围游走
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = ((-80 + sin(t1x * PI.toFloat()) * 90).dp),
                    y = ((-110 + sin(t1y * PI.toFloat()) * 85).dp)
                )
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
                .offset(
                    x = ((90 - sin(t2x * PI.toFloat()) * 90).dp),
                    y = ((40 + sin(t2y * PI.toFloat()) * 80).dp)
                )
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
                .offset(
                    x = ((-70 + sin(t3x * PI.toFloat()) * 95).dp),
                    y = ((70 + sin(t3y * PI.toFloat()) * 85).dp)
                )
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
                .offset(
                    x = ((-90 - sin(t4x * PI.toFloat()) * 85).dp),
                    y = ((-50 + sin(t4y * PI.toFloat()) * 80).dp)
                )
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
                .offset(
                    x = (sin(t5x * PI.toFloat()) * 55).dp,
                    y = (sin(t5y * PI.toFloat()) * 50).dp
                )
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
                .offset(
                    x = (sin(t6x * PI.toFloat()) * 60).dp,
                    y = ((100 + sin(t6y * PI.toFloat()) * 55).dp)
                )
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
    }
}
