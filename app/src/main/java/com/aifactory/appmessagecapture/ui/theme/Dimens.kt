package com.aifactory.appmessagecapture.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全局组件间距标准：所有界面主栏组件之间、列表卡片之间的纵向间距统一使用此值，
 * 与账单列表的卡片间距一致。后续想全局调整（如 6dp → 4dp），只改这一个常量；
 * 卡片列表内部的半间距用 [ComponentGap] / 2 派生，无需另行定义。
 */
val ComponentGap: Dp = 6.dp
