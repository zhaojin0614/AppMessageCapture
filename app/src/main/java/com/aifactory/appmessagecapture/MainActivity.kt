package com.aifactory.appmessagecapture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.birthday.ui.BirthdayScreen
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.birthday.widget.BirthdayWidget
import com.aifactory.appmessagecapture.service.MessageCaptureService
import com.aifactory.appmessagecapture.ui.BillScreen
import com.aifactory.appmessagecapture.ui.MainScreen
import com.aifactory.appmessagecapture.ui.components.AmbientBackground
import com.aifactory.appmessagecapture.ui.components.GlassBackdropRoot
import com.aifactory.appmessagecapture.ui.components.isDarkTheme
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassFill
import com.aifactory.appmessagecapture.ui.theme.AppMessageCaptureTheme
import com.aifactory.appmessagecapture.ui.theme.GradientBrandStart
import com.aifactory.appmessagecapture.ui.theme.GradientBrandEnd

enum class AppTab(val label: String, val icon: ImageVector, val iconFilled: ImageVector) {
    Messages("消息", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    Bills("记账", Icons.Outlined.Receipt, Icons.Filled.Receipt),
    Birthday("生日", Icons.Outlined.Cake, Icons.Filled.Cake)
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAVIGATE_TO_TAB = "navigate_to_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Wake up the notification listener service to trigger onCreate() -> requestRebind()
        val serviceIntent = Intent(this, MessageCaptureService::class.java)
        startService(serviceIntent)
        enableEdgeToEdge()

        val navigateToTab = intent.getStringExtra(EXTRA_NAVIGATE_TO_TAB)

        setContent {
            AppMessageCaptureTheme {
                MainApp(initialTab = navigateToTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainApp(initialTab: String? = null) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val context = LocalContext.current
    // Widget 点击跳转：自动切换到生日 Tab，并强制刷新 widget
    LaunchedEffect(initialTab) {
        when (initialTab) {
            "birthday" -> {
                selectedTab = 2
                try {
                    BirthdayWidget.updateAll(context)
                    BirthdayLog.i("[MainApp] BirthdayWidget.updateAll triggered after widget click.")
                } catch (e: Exception) {
                    BirthdayLog.logException("[MainApp] BirthdayWidget.updateAll", e)
                }
            }
            "bills" -> selectedTab = 1
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
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                }
            ) { _ ->
                // No bottom padding: like the report screen, tab content
                // extends behind the floating nav pill and scrolls beneath it.
                when (selectedTab) {
                    0 -> MainScreen()
                    1 -> BillScreen()
                    2 -> BirthdayScreen()
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
        Row(
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
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEachIndexed { index, tab ->
                SoftNavItem(
                    tab = tab,
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun SoftNavItem(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (selected) Modifier.border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    RoundedCornerShape(22.dp)
                ) else Modifier
            )
            .background(
                brush = if (selected) {
                    Brush.horizontalGradient(listOf(GradientBrandStart, GradientBrandEnd))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
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
