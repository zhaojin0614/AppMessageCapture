package com.aifactory.appmessagecapture.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.BuildConfig
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.ui.theme.ComponentGap

/**
 * 「我的」壳层页：全局能力的宿主，不属于任何功能模块、不可被关闭。
 *
 * 设置入口常驻于此——功能模块的开关都在设置页「功能开关」里，若入口寄生在
 * 可关闭的模块（此前挂在记账页顶栏），关闭该模块会导致设置不可达形成死锁
 * （详见 docs/设计方案-全局设置与我的页.md）。主流 App（微信/支付宝）均采用
 * 常驻「我的」Tab 承载设置入口。
 */
@Composable
fun MineScreen(modifier: Modifier = Modifier) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // 头部：应用图标 + 名称 + 版本
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = ComponentGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "捕账",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v" + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsGroup("通用") {
            SettingsNavigateRow(
                icon = Icons.Default.Settings,
                title = "设置",
                value = "功能开关 · 外观 · 数据",
                onClick = { showSettings = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
