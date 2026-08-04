package com.aifactory.appmessagecapture.birthday.ui

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifactory.appmessagecapture.birthday.service.BirthdayAlarmReceiver
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * 全屏生日闹钟 Activity。
 *
 * 触发时：
 * 1. 点亮屏幕并在锁屏上显示
 * 2. 持续播放默认闹钟铃声
 * 3. 展示亲友姓名和将满岁数
 * 4. 用户点击"知道了"后关闭铃声并结束 Activity
 */
class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BirthdayLog.i("[AlarmActivity] onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        val name = intent.getStringExtra(BirthdayAlarmReceiver.EXTRA_BIRTHDAY_NAME) ?: "亲友"
        val ageTurning = intent.getIntExtra(BirthdayAlarmReceiver.EXTRA_AGE_TURNING, -1)

        BirthdayLog.logMethodCall(
            "[AlarmActivity] received",
            mapOf("name" to name, "ageTurning" to ageTurning)
        )

        playAlarmSound()

        setContent {
            BirthdayAlarmScreen(
                name = name,
                ageTurning = ageTurning,
                onDismiss = { dismissAlarm() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        BirthdayLog.i("[AlarmActivity] onDestroy")
    }

    private fun playAlarmSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
            BirthdayLog.i("[AlarmActivity] Alarm sound started.")
        } catch (e: Exception) {
            BirthdayLog.logException("[AlarmActivity] playAlarmSound", e)
        }
    }

    private fun stopAlarmSound() {
        try {
            ringtone?.stop()
            ringtone = null
            BirthdayLog.i("[AlarmActivity] Alarm sound stopped.")
        } catch (e: Exception) {
            BirthdayLog.logException("[AlarmActivity] stopAlarmSound", e)
        }
    }

    private fun dismissAlarm() {
        stopAlarmSound()
        finish()
    }
}

// =============================================================================
// 生日提醒弹窗 —— Canvas 绘制蛋糕 + 气球 + 彩纸纸屑
// =============================================================================

// ---- 色彩系统（Soft UI 薄荷青品牌色系） ----
private val BgTop          = Color(0xFFEAF7F4)  // 浅薄荷绿
private val BgBottom       = Color(0xFFE9F0FA)  // 浅雾蓝
private val CardSurface    = Color(0xFFFAFAFA)
private val NameText       = Color(0xFF2D2D2D)
private val AgeText        = Color(0xFF0B6E60)  // 品牌深青
private val SubText        = Color(0xFFAAAAAA)
private val SepStart       = Color(0xFF0FA18D)  // 品牌主青
private val SepEnd         = Color(0xFF5BC0B4)  // 品牌渐变终点
private val BtnStart       = Color(0xFF0FA18D)  // 品牌主青
private val BtnEnd         = Color(0xFF5BC0B4)  // 品牌渐变终点

// 蛋糕色彩
private val FrostingWhite  = Color(0xFFFFF8F0)
private val FrostingOrange = Color(0xFFF0A24A)  // 暖杏色
private val CakeBody       = Color(0xFFE0A860)
private val CakeDark       = Color(0xFFCC8E48)
private val PlateColor     = Color(0xFFF0EBE2)
private val PlateShadow    = Color(0x15000000)
private val DripColor      = Color(0xFFFFB878)
private val CandleColors   = listOf(
    Color(0xFF0FA18D), Color(0xFF5C7FB8), Color(0xFFEE7BA6),
    Color(0xFFF2B84B), Color(0xFFA97BD6)
)
private val FlameOuter     = Color(0xFFFFB300)
private val FlameInner     = Color(0xFFFFF176)
private val FlameCore      = Color(0xFFFFFDE7)

// 气球色彩
private val BalloonGold    = Color(0xFFFFD54F)
private val BalloonPink    = Color(0xFFF48FB1)
private val BalloonOrange  = Color(0xFFF0A24A)  // 暖杏色
private val BalloonPeach   = Color(0xFFEFB8A8)  // 柔和蜜桃
private val BalloonString  = Color(0x60999999)

// 彩纸纸屑色彩
private val ConfettiColors = listOf(
    Color(0xFFFFD700), Color(0xFFFF6B9D), Color(0xFF0FA18D),
    Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFAB47BC),
    Color(0xFFFFCA28), Color(0xFFEF5350)
)

/** 彩纸粒子数据 */
private data class Confetti(
    val x: Float,
    val speedY: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
    val phaseOffset: Float,
    val shape: Int  // 0=矩形, 1=圆形, 2=十字星
)

// =============================================================================
// 主界面
// =============================================================================

/**
 * 生日提醒弹窗主界面
 *
 * 布局层次：
 * 1. 柔和渐变背景（浅薄荷绿 → 浅雾蓝）
 * 2. 半透明装饰气球 + 飘落彩纸（营造庆典氛围）
 * 3. 居中白色卡片（圆角 28dp、柔和阴影）
 *    - 顶部：Canvas 绘制双层蛋糕 + 蜡烛 + 脉动火焰光晕
 *    - 中部：姓名（深灰）+ 年龄（大号品牌深青粗体）+ 渐变分隔线 + 副文本
 *    - 底部：品牌青渐变"知道了"按钮（带阴影）
 */
@Composable
private fun BirthdayAlarmScreen(
    name: String,
    ageTurning: Int,
    onDismiss: () -> Unit
) {
    // 入场动画
    var appeared by remember { mutableFloatStateOf(0f) }
    val cardAlpha by animateFloatAsState(
        targetValue = appeared,
        animationSpec = tween(durationMillis = 800),
        label = "cardAlpha"
    )
    val cardSlideY by animateFloatAsState(
        targetValue = if (appeared > 0.3f) 0f else 80f,
        animationSpec = tween(durationMillis = 800),
        label = "cardSlideY"
    )

    LaunchedEffect(Unit) {
        delay(200)
        appeared = 1f
    }

    // 彩纸粒子
    val confettiList = remember {
        List(30) {
            Confetti(
                x = Random.nextFloat(),
                speedY = 0.12f + Random.nextFloat() * 0.35f,
                size = 3f + Random.nextFloat() * 6f,
                color = ConfettiColors[Random.nextInt(ConfettiColors.size)],
                rotationSpeed = (Random.nextFloat() - 0.5f) * 400f,
                phaseOffset = Random.nextFloat() * 360f,
                shape = Random.nextInt(3)
            )
        }
    }

    // 彩带
    val ribbonList = remember {
        List(8) {
            Ribbon(
                x = 0.05f + Random.nextFloat() * 0.9f,
                speedY = 0.06f + Random.nextFloat() * 0.14f,
                color = ConfettiColors[Random.nextInt(ConfettiColors.size)],
                width = 3f + Random.nextFloat() * 3f,
                waveAmp = 12f + Random.nextFloat() * 22f,
                phaseOffset = Random.nextFloat() * 360f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val timeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeProgress"
    )

    // 蜡烛火焰脉动动画
    val flamePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flamePhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        // ① 背景装饰气球
        BackgroundBalloons()

        // ② 彩带飘落层
        RibbonLayer(ribbonList, timeProgress)

        // ③ 彩纸粒子层
        ConfettiLayer(confettiList, timeProgress)

        // ④ 居中卡片 + 蛋糕插画
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 蛋糕插画（叠在卡片上边缘）
            BirthdayCakeIllustration(
                modifier = Modifier.size(width = 220.dp, height = 190.dp),
                flamePhase = flamePhase
            )

            Spacer(Modifier.height(8.dp))

            // 白色卡片
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.88f)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardSurface)
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .graphicsLayer {
                        alpha = cardAlpha
                        translationY = cardSlideY
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "生日提醒",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NameText,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(Modifier.height(8.dp))

                // 主文案（大号品牌深青粗体）
                Text(
                    text = name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AgeText,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(Modifier.height(8.dp))

                // 渐变分隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(SepStart, SepEnd)))
                )

                Spacer(Modifier.height(8.dp))

                // 副文本（浅灰）
                val mainText = if (ageTurning > 0) {
                    "今天过生日，满${ageTurning}岁啦！"
                } else {
                    "今天过生日！"
                }
                Text(
                    text = mainText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = SubText,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // 渐变按钮
                Box(
                    modifier = Modifier
                        .widthIn(min = 200.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50),
                            clip = false
                        )
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(BtnStart, BtnEnd)))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 48.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "知道了",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// 双层蛋糕插画（Canvas 绘制）
// =============================================================================

/**
 * 使用 Canvas 绘制的双层生日蛋糕。
 *
 * 结构：暖色光晕 → 底盘 → 下层蛋糕 → 暖杏色糖霜滴落 →
 *       上层蛋糕 → 白色糖霜滴落 → 金色装饰点 →
 *       5 根彩色蜡烛 → 脉动火焰（光晕 + 外焰 + 内焰 + 火芯）。
 */
@Composable
private fun BirthdayCakeIllustration(
    modifier: Modifier = Modifier,
    flamePhase: Float = 0f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // ---- 蛋糕背后的暖色光晕 ----
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x18FFB74D), Color.Transparent),
                center = Offset(cx, h * 0.55f),
                radius = w * 0.45f
            ),
            center = Offset(cx, h * 0.55f),
            radius = w * 0.45f
        )

        // ---- ① 底盘 ----
        val plateY = h * 0.82f
        val plateW = w * 0.72f
        val plateH = h * 0.06f
        drawOval(
            color = PlateShadow,
            topLeft = Offset(cx - plateW / 2f, plateY + 4f),
            size = Size(plateW, plateH)
        )
        drawOval(
            color = PlateColor,
            topLeft = Offset(cx - plateW / 2f, plateY),
            size = Size(plateW, plateH)
        )

        // ---- ② 下层蛋糕 ----
        val bottomW = w * 0.56f
        val bottomH = h * 0.22f
        val bottomY = plateY - bottomH
        drawRoundRect(
            color = CakeBody,
            topLeft = Offset(cx - bottomW / 2f, bottomY),
            size = Size(bottomW, bottomH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = CakeDark,
            topLeft = Offset(cx - bottomW / 2f, bottomY + bottomH - 5f),
            size = Size(bottomW, 5f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // ---- ③ 下层糖霜（橙色 + 滴落） ----
        val dripH = h * 0.045f
        drawRoundRect(
            color = FrostingOrange,
            topLeft = Offset(cx - bottomW / 2f, bottomY),
            size = Size(bottomW, dripH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        val dripCount = 7
        for (i in 0 until dripCount) {
            val dx = cx - bottomW / 2f + bottomW / (dripCount + 1) * (i + 1)
            val dLen = h * (0.02f + (i % 3) * 0.012f)
            val dW = bottomW * 0.05f
            drawRoundRect(
                color = DripColor,
                topLeft = Offset(dx - dW / 2f, bottomY + dripH - 2f),
                size = Size(dW, dLen),
                cornerRadius = CornerRadius(dW / 2f, dW / 2f)
            )
        }

        // ---- ④ 上层蛋糕 ----
        val topW = w * 0.42f
        val topH = h * 0.18f
        val topY = bottomY - topH + 4f
        drawRoundRect(
            color = CakeBody,
            topLeft = Offset(cx - topW / 2f, topY),
            size = Size(topW, topH),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = CakeDark,
            topLeft = Offset(cx - topW / 2f, topY + topH - 4f),
            size = Size(topW, 4f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // ---- ⑤ 上层糖霜（白色 + 滴落） ----
        val topDripH = h * 0.04f
        drawRoundRect(
            color = FrostingWhite,
            topLeft = Offset(cx - topW / 2f, topY),
            size = Size(topW, topDripH),
            cornerRadius = CornerRadius(8f, 8f)
        )
        val topDripCount = 5
        for (i in 0 until topDripCount) {
            val dx = cx - topW / 2f + topW / (topDripCount + 1) * (i + 1)
            val dLen = h * (0.018f + (i % 2) * 0.01f)
            val dW = topW * 0.06f
            drawRoundRect(
                color = FrostingWhite,
                topLeft = Offset(dx - dW / 2f, topY + topDripH - 2f),
                size = Size(dW, dLen),
                cornerRadius = CornerRadius(dW / 2f, dW / 2f)
            )
        }

        // ---- ⑥ 金色装饰圆点 ----
        val dotY = topY + topH * 0.58f
        for (i in 0 until 5) {
            val dx = cx - topW * 0.35f + (topW * 0.7f / 4) * i
            drawCircle(
                color = Color(0xFFFFD700),
                radius = w * 0.009f,
                center = Offset(dx, dotY)
            )
        }

        // ---- ⑦ 蜡烛 + 火焰 ----
        val candleCount = 5
        val candleW = w * 0.024f
        val candleH = h * 0.14f
        val candleY = topY - candleH + 6f
        val spacing = if (candleCount > 1) topW * 0.7f / (candleCount - 1) else 0f
        val startX = cx - topW * 0.35f

        for (i in 0 until candleCount) {
            val x = startX + spacing * i
            val candleColor = CandleColors[i % CandleColors.size]

            // 蜡烛主体
            drawRoundRect(
                color = candleColor,
                topLeft = Offset(x - candleW / 2f, candleY),
                size = Size(candleW, candleH),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // 条纹装饰
            drawRoundRect(
                color = candleColor.copy(alpha = 0.45f),
                topLeft = Offset(x - candleW * 0.15f, candleY + candleH * 0.1f),
                size = Size(candleW * 0.3f, candleH * 0.8f),
                cornerRadius = CornerRadius(1f, 1f)
            )
            // 烛芯
            val wickTopY = candleY - h * 0.02f
            drawLine(
                color = Color(0xFF5D4037),
                start = Offset(x, candleY),
                end = Offset(x, wickTopY),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )

            // ---- ⑧ 火焰 ----
            val flameSize = w * 0.018f
            val pulse = (sin(flamePhase * Math.PI * 2 + i * 1.2) * 0.5f + 0.5f).toFloat()
            val flameBaseY = wickTopY
            val arcTop = flameBaseY - flameSize * 2.5f
            val arcR = flameSize

            // 柔和光晕（脉动）
            val glowR = flameSize * (3.5f + pulse * 1.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x28FFB300), Color.Transparent),
                    center = Offset(x, arcTop + arcR),
                    radius = glowR
                ),
                center = Offset(x, arcTop + arcR),
                radius = glowR
            )

            // 外焰（橙色水滴形 = 顶部半圆弧 + 底部三角）
            drawArc(
                color = FlameOuter,
                startAngle = 0f,
                sweepAngle = -180f,
                useCenter = false,
                topLeft = Offset(x - arcR, arcTop),
                size = Size(arcR * 2, arcR * 2)
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x - arcR * 0.87f, arcTop + arcR)
                    lineTo(x + arcR * 0.87f, arcTop + arcR)
                    lineTo(x, flameBaseY + flameSize * 0.4f)
                    close()
                },
                color = FlameOuter
            )

            // 内焰（黄色）
            val innerR = arcR * 0.6f
            drawArc(
                color = FlameInner,
                startAngle = 0f,
                sweepAngle = -180f,
                useCenter = false,
                topLeft = Offset(x - innerR, arcTop + arcR - innerR),
                size = Size(innerR * 2, innerR * 2)
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x - innerR * 0.87f, arcTop + arcR)
                    lineTo(x + innerR * 0.87f, arcTop + arcR)
                    lineTo(x, flameBaseY)
                    close()
                },
                color = FlameInner
            )

            // 火芯（白色亮点）
            drawCircle(
                color = FlameCore,
                radius = flameSize * 0.22f,
                center = Offset(x, arcTop + arcR * 0.6f)
            )
        }
    }
}

// =============================================================================
// 背景装饰气球
// =============================================================================

/**
 * 背景层：6 个半透明气球 + 细绳，模拟景深效果。
 */
@Composable
private fun BackgroundBalloons() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        data class BgBalloon(
            val xFrac: Float, val yFrac: Float,
            val radiusFrac: Float,
            val color: Color, val alpha: Float
        )

        val balloons = listOf(
            BgBalloon(0.08f, 0.10f, 0.065f, BalloonGold,   0.30f),
            BgBalloon(0.88f, 0.06f, 0.072f, BalloonPink,   0.32f),
            BgBalloon(0.04f, 0.35f, 0.058f, BalloonOrange, 0.28f),
            BgBalloon(0.92f, 0.28f, 0.052f, BalloonGold,   0.35f),
            BgBalloon(0.78f, 0.78f, 0.060f, BalloonPeach,  0.25f),
            BgBalloon(0.18f, 0.75f, 0.050f, BalloonPink,   0.30f)
        )

        balloons.forEach { b ->
            val bx = w * b.xFrac
            val by = h * b.yFrac
            // 用较短边计算半径，确保气球接近圆形
            val r = minOf(w, h) * b.radiusFrac
            // 气球略高于宽（真实气球比例 1:1.15）
            val rx = r
            val ry = r * 1.15f

            // 气球本体
            drawOval(
                color = b.color.copy(alpha = b.alpha),
                topLeft = Offset(bx - rx, by - ry),
                size = Size(rx * 2, ry * 2)
            )
            // 高光
            drawOval(
                color = Color.White.copy(alpha = b.alpha * 0.45f),
                topLeft = Offset(bx - rx * 0.35f, by - ry * 0.55f),
                size = Size(rx * 0.4f, ry * 0.3f)
            )
            // 绳结小三角
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(bx, by + ry)
                    lineTo(bx - w * 0.005f, by + ry + h * 0.008f)
                    lineTo(bx + w * 0.005f, by + ry + h * 0.008f)
                    close()
                },
                color = b.color.copy(alpha = b.alpha * 0.8f)
            )
            // 细绳
            drawLine(
                color = BalloonString,
                start = Offset(bx, by + ry + h * 0.008f),
                end = Offset(bx + w * 0.01f, by + ry + h * 0.055f),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round
            )
        }
    }
}

// =============================================================================
// 彩纸粒子层
// =============================================================================

/**
 * 全局飘落的五彩纸屑（矩形、圆形、十字星三种形状）。
 */
@Composable
private fun ConfettiLayer(confettiList: List<Confetti>, timeProgress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height

        confettiList.forEach { c ->
            val rawY = (timeProgress * c.speedY + c.phaseOffset / 360f) % 1f
            val y = rawY * (ch + 30f) - 15f
            val wobble = sin((rawY * 360f + c.phaseOffset) * Math.PI / 180.0).toFloat() * 18f
            val x = c.x * cw + wobble

            when (c.shape) {
                0 -> { // 矩形
                    drawRect(
                        color = c.color.copy(alpha = 0.75f),
                        topLeft = Offset(x - c.size * 0.5f, y - c.size * 0.3f),
                        size = Size(c.size, c.size * 0.6f)
                    )
                }
                1 -> { // 圆形
                    drawCircle(
                        color = c.color.copy(alpha = 0.7f),
                        radius = c.size * 0.5f,
                        center = Offset(x, y)
                    )
                }
                else -> { // 十字星
                    val half = c.size * 0.5f
                    drawLine(
                        color = c.color.copy(alpha = 0.75f),
                        start = Offset(x - half, y),
                        end = Offset(x + half, y),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = c.color.copy(alpha = 0.75f),
                        start = Offset(x, y - half),
                        end = Offset(x, y + half),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// =============================================================================
// 彩带飘落层
// =============================================================================

/** 彩带数据 */
private data class Ribbon(
    val x: Float,
    val speedY: Float,
    val color: Color,
    val width: Float,
    val waveAmp: Float,
    val phaseOffset: Float
)

/**
 * 彩带飘落效果。
 *
 * 每条彩带用 quadraticBezierTo 绘制 S 形波浪曲线，
 * 模拟真实的丝带/卷带在空中缓缓飘落的效果。
 */
@Composable
private fun RibbonLayer(ribbonList: List<Ribbon>, timeProgress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height

        ribbonList.forEach { r ->
            val progress = (timeProgress * r.speedY + r.phaseOffset / 360f) % 1f
            val headY = progress * (ch + 160f) - 80f
            val ribbonLen = 110f + r.waveAmp * 2f
            val baseW = r.width

            // 沿彩带中心线采样
            val segments = 16
            val phase = r.phaseOffset * Math.PI / 180.0
            val points = (0..segments).map { i ->
                val t = i.toFloat() / segments
                val y = headY + t * ribbonLen
                val wave = sin(t * Math.PI * 3 + progress * Math.PI * 4 + phase)
                val x = r.x * cw + (wave * r.waveAmp * (1f - t * 0.3f)).toFloat()
                Offset(x, y)
            }

            // 计算左右边缘（宽度随 sin 扭转变化，模拟丝带翻转）
            val rightEdge = points.mapIndexed { i, p ->
                val t = i.toFloat() / segments
                val twist = (sin(t * Math.PI * 3 + progress * Math.PI * 4 + phase) * 0.5f + 0.5f).toFloat()
                val halfW = baseW * (0.35f + 0.65f * twist) / 2f
                Offset(p.x + halfW, p.y)
            }
            val leftEdge = points.mapIndexed { i, p ->
                val t = i.toFloat() / segments
                val twist = (sin(t * Math.PI * 3 + progress * Math.PI * 4 + phase) * 0.5f + 0.5f).toFloat()
                val halfW = baseW * (0.35f + 0.65f * twist) / 2f
                Offset(p.x - halfW, p.y)
            }

            // 构建填充轮廓：右边缘从上到下 → 左边缘从下到上 → 闭合
            val outline = androidx.compose.ui.graphics.Path().apply {
                moveTo(rightEdge[0].x, rightEdge[0].y)
                for (i in 1 until rightEdge.size) lineTo(rightEdge[i].x, rightEdge[i].y)
                for (i in leftEdge.indices.reversed()) lineTo(leftEdge[i].x, leftEdge[i].y)
                close()
            }

            // 绘制彩带主体
            drawPath(path = outline, color = r.color.copy(alpha = 0.72f))

            // 高光条纹（窄带，偏右，模拟光泽）
            val hlPath = androidx.compose.ui.graphics.Path().apply {
                val hlOff = baseW * 0.15f
                moveTo(points[0].x + hlOff, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x + hlOff, points[i].y)
                for (i in points.indices.reversed()) lineTo(points[i].x + hlOff * 0.4f, points[i].y)
                close()
            }
            drawPath(path = hlPath, color = Color.White.copy(alpha = 0.22f))
        }
    }
}
