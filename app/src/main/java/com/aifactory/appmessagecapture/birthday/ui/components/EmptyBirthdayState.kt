package com.aifactory.appmessagecapture.birthday.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aifactory.appmessagecapture.ui.components.SoftEmptyState

/**
 * 生日列表空状态展示。
 *
 * 复用 Soft UI 共享组件 SoftEmptyState，保持居中布局。
 */
@Composable
fun EmptyBirthdayState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SoftEmptyState(
            icon = Icons.Default.Cake,
            title = "还没有记录生日",
            subtitle = "点击右下角按钮添加亲友生日，支持公历和农历"
        )
    }
}
