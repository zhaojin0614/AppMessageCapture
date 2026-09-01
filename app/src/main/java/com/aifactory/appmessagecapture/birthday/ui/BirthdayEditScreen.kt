package com.aifactory.appmessagecapture.birthday.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.data.ReminderType
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.ui.components.PillToggle
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassFill
import com.aifactory.appmessagecapture.ui.components.gradientBrush

/**
 * 添加/编辑生日记录页。
 *
 * 液态玻璃风格：头像英雄卡（首字随姓名实时变化）、玻璃分段控件（公历/农历）、
 * 玻璃下拉（月/日/时/分）、提醒方式芯片组，替换原先的 M3 描边输入框 +
 * 单选按钮列表的朴素样式。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayEditScreen(
    modifier: Modifier = Modifier,
    viewModel: BirthdayViewModel,
    birthdayId: Int?,
    onNavigateBack: () -> Unit
) {
    val isEdit = birthdayId != null && birthdayId > 0

    BackHandler { onNavigateBack() }

    // 表单状态
    var name by rememberSaveable { mutableStateOf("") }
    var isLunar by rememberSaveable { mutableStateOf(false) }
    var birthYear by rememberSaveable { mutableStateOf("") }
    var birthMonth by rememberSaveable { mutableIntStateOf(1) }
    var birthDay by rememberSaveable { mutableIntStateOf(1) }
    var reminderType by rememberSaveable { mutableStateOf(ReminderType.ON_DAY) }
    var reminderHour by rememberSaveable { mutableIntStateOf(9) }
    var reminderMinute by rememberSaveable { mutableIntStateOf(0) }

    // 加载已有数据
    LaunchedEffect(birthdayId) {
        if (isEdit) {
            val entity = viewModel.getById(birthdayId!!)
            entity?.let {
                BirthdayLog.i("[BirthdayEditScreen] Loaded entity for edit: %s", it)
                name = it.name
                isLunar = it.isLunar
                birthYear = it.birthYear?.toString() ?: ""
                birthMonth = it.birthMonth
                birthDay = it.birthDay
                reminderType = it.reminderType
                it.reminderTime?.let { time ->
                    val parts = time.split(":")
                    if (parts.size == 2) {
                        reminderHour = parts[0].toIntOrNull() ?: 9
                        reminderMinute = parts[1].toIntOrNull() ?: 0
                    }
                }
            }
        }
    }

    fun saveBirthday() {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val yearInt = birthYear.toIntOrNull()
        val reminderTimeStr = if (reminderType == ReminderType.NONE) null
        else String.format("%02d:%02d", reminderHour, reminderMinute)

        val entity = BirthdayEntity(
            id = birthdayId ?: 0,
            name = trimmedName,
            isLunar = isLunar,
            birthYear = yearInt,
            birthMonth = birthMonth,
            birthDay = birthDay,
            reminderType = reminderType,
            reminderTime = reminderTimeStr
        )
        viewModel.save(entity) { onNavigateBack() }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) "编辑生日" else "添加生日",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 头像英雄卡：首字头像 + 姓名 ────────────────────────────
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                contentPadding = 20.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Text(
                                text = trimmed.take(1),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (name.isEmpty()) {
                                    Text(
                                        text = "点此输入亲友姓名",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            // ── 生日日期卡 ─────────────────────────────────────────────
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                contentPadding = 16.dp
            ) {
                Column {
                    EditSectionHeader(icon = Icons.Default.Cake, title = "生日日期")
                    Spacer(modifier = Modifier.height(12.dp))
                    PillToggle(
                        options = listOf(
                            "公历" to MaterialTheme.colorScheme.primary,
                            "农历" to MaterialTheme.colorScheme.primary
                        ),
                        selectedIndex = if (isLunar) 1 else 0,
                        onSelect = { isLunar = it == 1 }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassNumberDropdown(
                            selected = birthMonth,
                            range = 1..12,
                            format = { "$it 月" },
                            onSelected = { birthMonth = it },
                            modifier = Modifier.weight(1f)
                        )
                        GlassNumberDropdown(
                            selected = birthDay,
                            range = 1..31,
                            format = { "$it 日" },
                            onSelected = { birthDay = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    YearField(
                        value = birthYear,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("\\d{0,4}"))) birthYear = it }
                    )
                }
            }

            // ── 提醒设置卡 ─────────────────────────────────────────────
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                contentPadding = 16.dp
            ) {
                Column {
                    EditSectionHeader(icon = Icons.Default.Notifications, title = "提醒设置")
                    Spacer(modifier = Modifier.height(12.dp))
                    // 提醒方式芯片：两行 3+2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReminderChip(
                            label = "不提醒",
                            selected = reminderType == ReminderType.NONE,
                            onClick = { reminderType = ReminderType.NONE },
                            modifier = Modifier.weight(1f)
                        )
                        ReminderChip(
                            label = "当天",
                            selected = reminderType == ReminderType.ON_DAY,
                            onClick = { reminderType = ReminderType.ON_DAY },
                            modifier = Modifier.weight(1f)
                        )
                        ReminderChip(
                            label = "提前1天",
                            selected = reminderType == ReminderType.ONE_DAY_BEFORE,
                            onClick = { reminderType = ReminderType.ONE_DAY_BEFORE },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReminderChip(
                            label = "提前3天",
                            selected = reminderType == ReminderType.THREE_DAYS_BEFORE,
                            onClick = { reminderType = ReminderType.THREE_DAYS_BEFORE },
                            modifier = Modifier.weight(1f)
                        )
                        ReminderChip(
                            label = "提前1周",
                            selected = reminderType == ReminderType.ONE_WEEK_BEFORE,
                            onClick = { reminderType = ReminderType.ONE_WEEK_BEFORE },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 提醒时间（若提醒类型不为 NONE）
                    if (reminderType != ReminderType.NONE) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "提醒时间",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GlassNumberDropdown(
                                selected = reminderHour,
                                range = 0..23,
                                format = { "%02d 时".format(it) },
                                onSelected = { reminderHour = it },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ":",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GlassNumberDropdown(
                                selected = reminderMinute,
                                range = 0..59,
                                format = { "%02d 分".format(it) },
                                onSelected = { reminderMinute = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── 操作按钮：玻璃取消 + 主色保存 ──────────────────────────
            val canSave = name.trim().isNotBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(glassFill())
                        .border(glassBorder(), RoundedCornerShape(14.dp))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "取消",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .alpha(if (canSave) 1f else 0.45f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(gradientBrush(MaterialTheme.colorScheme.primary, alpha = 0.95f))
                        .clickable(enabled = canSave) { saveBirthday() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEdit) "保存" else "添加",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// -------------------------------------------------------------------------
// 子组件
// -------------------------------------------------------------------------

/** 卡片区标题：主题色小图标 + 标题 */
@Composable
private fun EditSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** 玻璃下拉选择：居中显示，选中项带主题色 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassNumberDropdown(
    selected: Int,
    range: IntRange,
    format: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .clip(RoundedCornerShape(12.dp))
                .background(glassFill())
                .border(glassBorder(), RoundedCornerShape(12.dp))
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = format(selected),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { value ->
                DropdownMenuItem(
                    text = { Text(format(value), fontSize = 14.sp) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 提醒方式芯片：选中态主题色渐变填充，未选中玻璃填充 */
@Composable
private fun ReminderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) gradientBrush(MaterialTheme.colorScheme.primary, alpha = 0.92f)
                else SolidColor(glassFill())
            )
            .border(glassBorder(), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** 出生年份玻璃输入行：左标签 + 右数字输入（选填） + 「年」后缀 */
@Composable
private fun YearField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(glassFill())
            .border(glassBorder(), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "出生年份",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.width(110.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "选填",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "年",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
