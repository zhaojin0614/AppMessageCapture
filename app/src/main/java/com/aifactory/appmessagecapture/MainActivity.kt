package com.aifactory.appmessagecapture

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.birthday.ui.BirthdayScreen
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.birthday.widget.BirthdayWidget
import com.aifactory.appmessagecapture.features.FeatureModule
import com.aifactory.appmessagecapture.features.FeatureRepository
import com.aifactory.appmessagecapture.service.BillNotificationHelper
import com.aifactory.appmessagecapture.service.MessageCaptureService
import com.aifactory.appmessagecapture.ui.BillScreen
import com.aifactory.appmessagecapture.ui.MainScreen
import com.aifactory.appmessagecapture.ui.MineScreen
import com.aifactory.appmessagecapture.ui.components.AmbientBackground
import com.aifactory.appmessagecapture.ui.components.GlassBackdropRoot
import com.aifactory.appmessagecapture.ui.components.SliderStiffness
import com.aifactory.appmessagecapture.ui.components.isDarkTheme
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassFill
import com.aifactory.appmessagecapture.ui.theme.AppMessageCaptureTheme
import com.aifactory.appmessagecapture.ui.theme.AccentColorRepository
import kotlinx.coroutines.launch

/**
 * 主导航 Tab。功能 Tab 绑定 [FeatureModule]（模块关闭后 Tab 隐藏，见 [MainApp]
 * 的 visibleTabs 过滤）；[Mine] 是壳层 Tab（feature = null），不参与模块开关
 * 过滤、永不可关——全局设置入口常驻其下，保证任意开关组合下设置可达
 * （修复：设置入口曾寄生在可关闭的记账 Tab，关闭「自动记账」后设置不可达）。
 */
enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val iconFilled: ImageVector,
    /** 绑定的功能模块；null = 壳层 Tab，不受功能开关影响 */
    val feature: FeatureModule?
) {
    Messages("消息", Icons.Outlined.Notifications, Icons.Filled.Notifications, FeatureModule.MESSAGES),
    Bills("记账", Icons.Outlined.Receipt, Icons.Filled.Receipt, FeatureModule.BILLS),
    Birthday("生日", Icons.Outlined.Cake, Icons.Filled.Cake, FeatureModule.BIRTHDAY),
    Mine("我的", Icons.Outlined.Person, Icons.Filled.Person, null)
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAVIGATE_TO_TAB = "navigate_to_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AccentColorRepository.init(this)
        // Wake up the notification listener service to trigger onCreate() -> requestRebind()
        val serviceIntent = Intent(this, MessageCaptureService::class.java)
        startService(serviceIntent)
        enableEdgeToEdge()

        val navigateToTab = intent.getStringExtra(EXTRA_NAVIGATE_TO_TAB)
        cancelBillNotification(intent)

        setContent {
            AppMessageCaptureTheme {
                MainApp(initialTab = navigateToTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        cancelBillNotification(intent)
    }

    /** 通过「去查看」打开时，清除对应的常驻账单通知 */
    private fun cancelBillNotification(intent: Intent) {
        val notificationId = intent.getIntExtra(
            BillNotificationHelper.EXTRA_CANCEL_NOTIFICATION_ID, -1
        )
        if (notificationId >= BillNotificationHelper.NOTIFICATION_ID_BASE) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }
    }
}

@Composable
fun MainApp(initialTab: String? = null) {
    // 功能开关：主导航只显示开启中的功能 Tab；「我的」是壳层 Tab 永远显示
    val disabledFeatures by FeatureRepository.disabled.collectAsState()
    val visibleTabs = remember(disabledFeatures) {
        AppTab.entries.filter { it.feature == null || it.feature !in disabledFeatures }
    }
    // 选中 Tab 按「身份」持久化（存 enum 名，随布局变化按名解析）：
    // 按索引追踪会被功能开关增删的 Tab 顶偏——如在「我的」上重新开启
    // 消息捕获，其后所有 Tab 索引右移，索引 2 会落到生日上；按名解析
    // 并对已被关闭的 Tab 回退到「我的」（壳层 Tab 永不消失）
    var selectedTabId by rememberSaveable { mutableStateOf(AppTab.Messages.name) }
    val selectedTab = visibleTabs.firstOrNull { it.name == selectedTabId } ?: AppTab.Mine

    val context = LocalContext.current
    // Widget 点击跳转：自动切换到生日 Tab，并强制刷新 widget
    // （目标模块已被关闭时保持当前 Tab 不动）
    LaunchedEffect(initialTab) {
        when (initialTab) {
            "birthday" -> if (AppTab.Birthday in visibleTabs) {
                selectedTabId = AppTab.Birthday.name
                try {
                    BirthdayWidget.updateAll(context)
                    BirthdayLog.i("[MainApp] BirthdayWidget.updateAll triggered after widget click.")
                } catch (e: Exception) {
                    BirthdayLog.logException("[MainApp] BirthdayWidget.updateAll", e)
                }
            }
            "bills" -> if (AppTab.Bills in visibleTabs) {
                selectedTabId = AppTab.Bills.name
            }
        }
    }

    // Single ambient background for the whole app: the bottom nav area and
    // every transparent screen share the same gradient layer, so the
    // floating nav pill has no solid strip on either side.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDarkTheme()) {
                        listOf(Color(0xFF162521), Color(0xFF1B2030), Color(0xFF211B16))
                    } else {
                        listOf(Color(0xFFD9F2EC), Color(0xFFE3E9F8), Color(0xFFF6EDE2))
                    }
                )
            )
    ) {
        AmbientBackground()
        // Real-time backdrop blur (iOS-style): screen content is captured
        // to an off-screen layer, blurred underneath, then redrawn sharp —
        // transparent glass components reveal the blurred copy.
        GlassBackdropRoot(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    SoftNavBar(
                        tabs = visibleTabs,
                        selectedIndex = visibleTabs.indexOf(selectedTab),
                        onSelect = { index -> selectedTabId = visibleTabs[index].name }
                    )
                }
            ) { _ ->
                // No bottom padding: like the report screen, tab content
                // extends behind the floating nav pill and scrolls beneath it.
                // SaveableStateProvider keeps each tab's scroll position and
                // remember state alive across tab switches (previously the
                // whole screen was disposed and lists reset to the top).
                // Crossfade：切 tab 时内容轻微淡入淡出，与滑块滑动节奏配合。
                val stateHolder = rememberSaveableStateHolder()
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(100, easing = FastOutSlowInEasing),
                    label = "tabContentFade"
                ) { tab ->
                    when (tab) {
                        AppTab.Messages -> stateHolder.SaveableStateProvider(key = "tab_messages") { MainScreen() }
                        AppTab.Bills -> stateHolder.SaveableStateProvider(key = "tab_bills") { BillScreen() }
                        AppTab.Birthday -> stateHolder.SaveableStateProvider(key = "tab_birthday") { BirthdayScreen() }
                        AppTab.Mine -> stateHolder.SaveableStateProvider(key = "tab_mine") { MineScreen() }
                    }
                }
            }
        }
    }
}

// ============================================================
// Soft floating pill navigation bar — signature of the new UI
// ============================================================

@Composable
private fun SoftNavBar(
    tabs: List<AppTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // 导航项几何（px，相对内容区原点）；首次布局测量后直接落位，之后切换才滑动
        // （模块开关增减 Tab 时随 tabs 重建，重新走一次「直接落位」）
        var itemLefts by remember(tabs) { mutableStateOf(FloatArray(tabs.size)) }
        var itemWidths by remember(tabs) { mutableStateOf(IntArray(tabs.size)) }
        var measured by remember(tabs) { mutableStateOf(false) }
        val sliderLeft = remember { Animatable(0f) }
        val sliderWidth = remember { Animatable(0f) }
        val accent = AccentColorRepository.current

        LaunchedEffect(itemLefts, itemWidths, selectedIndex) {
            if (!measured) return@LaunchedEffect
            // 防御：模块开关增减 Tab 的过渡帧里 selectedIndex 可能仍指向旧
            // 布局（如 4 项的尾部索引撞上 3 项数组），越界直接跳过本次动画，
            // 等 clamp 写回后随新 key 重启
            if (selectedIndex < 0 || selectedIndex >= itemLefts.size) return@LaunchedEffect
            val targetLeft = itemLefts[selectedIndex]
            val targetWidth = itemWidths[selectedIndex].toFloat()
            if (sliderWidth.value == 0f) {
                // 首次定位：直接落位，避免从 0 起步的开场滑入
                sliderLeft.snapTo(targetLeft)
                sliderWidth.snapTo(targetWidth)
            } else {
                // 滑块滑动：位置与宽度并行动画，≈200ms 到位
                launch {
                    sliderLeft.animateTo(
                        targetLeft,
                        spring(Spring.DampingRatioNoBouncy, SliderStiffness)
                    )
                }
                sliderWidth.animateTo(
                    targetWidth,
                    spring(Spring.DampingRatioNoBouncy, SliderStiffness)
                )
            }
        }

        Box(
            modifier = Modifier
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color(0x26000000),
                    spotColor = Color(0x33000000)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(glassFill())
                .border(glassBorder(), RoundedCornerShape(28.dp))
                .drawBehind {
                    // 滑块画在玻璃底色之上、导航项之下，切换时在两个 tab 之间滑动
                    if (!measured || sliderWidth.value <= 0f) return@drawBehind
                    val pad = 6.dp.toPx() // drawBehind 坐标系含容器内边距，需补回
                    val topLeft = Offset(pad + sliderLeft.value, pad)
                    val size = Size(sliderWidth.value, this.size.height - pad * 2)
                    val corner = CornerRadius(22.dp.toPx())
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(accent.primary, accent.gradientEnd)),
                        topLeft = topLeft,
                        size = size,
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = topLeft,
                        size = size,
                        cornerRadius = corner,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    SoftNavItem(
                        tab = tab,
                        selected = selectedIndex == index,
                        onGeometry = { left, width ->
                            if (itemLefts[index] != left || itemWidths[index] != width) {
                                val newLefts = itemLefts.copyOf(); newLefts[index] = left
                                val newWidths = itemWidths.copyOf(); newWidths[index] = width
                                itemLefts = newLefts
                                itemWidths = newWidths
                                measured = true
                            }
                        },
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SoftNavItem(
    tab: AppTab,
    selected: Boolean,
    onGeometry: (Float, Int) -> Unit,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .onGloballyPositioned { coords ->
                onGeometry(coords.positionInParent().x, coords.size.width)
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (selected) tab.iconFilled else tab.icon,
                contentDescription = tab.label,
                tint = if (selected) Color.White else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            // 标签瞬时切换（不做展开动画）：item 宽度在一次布局内到位，
            // 滑块只对「最终几何」做一次干净的滑动动画，
            // 若标签逐帧展开，滑块弹簧每帧被打断重启，观感卡顿
            if (selected) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}
