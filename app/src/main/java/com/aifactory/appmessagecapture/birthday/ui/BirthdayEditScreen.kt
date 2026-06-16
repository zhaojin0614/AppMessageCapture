package com.aifactory.appmessagecapture.birthday.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.data.ReminderType
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog

/**
 * 添加/编辑生日记录页。
 *
 * 支持输入姓名、公历/农历切换、月日选择、出生年份（可选）、提醒设置。
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

    Scaffold(
        modifier = modifier,
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 姓名
            CompactOutlinedField(
                value = name,
                onValueChange = { name = it },
                label = { Text("亲友姓名 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 公历 / 农历 切换
            CalendarTypeSelector(
                isLunar = isLunar,
                onToggle = { isLunar = it }
            )

            // 月 / 日 选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NumberDropdown(
                    label = "月份",
                    selected = birthMonth,
                    range = 1..12,
                    onSelected = { birthMonth = it },
                    modifier = Modifier.weight(1f)
                )
                NumberDropdown(
                    label = "日期",
                    selected = birthDay,
                    range = 1..31,
                    onSelected = { birthDay = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // 出生年份（可选）
            CompactOutlinedField(
                value = birthYear,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("\\d{0,4}"))) {
                        birthYear = it
                    }
                },
                label = { Text("出生年份（可选，用于计算岁数）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 提醒类型
            ReminderTypeSelector(
                selected = reminderType,
                onSelected = { reminderType = it }
            )

            // 提醒时间（若提醒类型不为 NONE）
            if (reminderType != ReminderType.NONE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "提醒时间",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    NumberDropdown(
                        label = "时",
                        selected = reminderHour,
                        range = 0..23,
                        onSelected = { reminderHour = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.bodyLarge)
                    NumberDropdown(
                        label = "分",
                        selected = reminderMinute,
                        range = 0..59,
                        onSelected = { reminderMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isBlank()) return@Button

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

                        viewModel.save(entity) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.trim().isNotBlank(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isEdit) "保存" else "添加")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// -------------------------------------------------------------------------
// 子组件
// -------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTypeSelector(
    isLunar: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "历法类型",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = !isLunar,
                        onClick = { onToggle(false) }
                    )
                    Text("公历（阳历）", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = isLunar,
                        onClick = { onToggle(true) }
                    )
                    Text("农历（阴历）", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberDropdown(
    label: String,
    selected: Int,
    range: IntRange,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = MaterialTheme.typography.bodyLarge
    val colors = OutlinedTextFieldDefaults.colors()
    val shape = RoundedCornerShape(16.dp)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        BasicTextField(
            value = "$selected",
            onValueChange = {},
            readOnly = true,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = "$selected",
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    label = { Text(label) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = colors,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = shape
                        )
                    }
                )
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { value ->
                DropdownMenuItem(
                    text = { Text("$value") },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTypeSelector(
    selected: ReminderType,
    onSelected: (ReminderType) -> Unit
) {
    Column {
        Text(
            text = "提醒方式",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selected == type,
                            onClick = { onSelected(type) }
                        )
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * 紧凑版 OutlinedTextField，缩小内部上下留白（8dp/8dp 代替默认 16dp/16dp）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = MaterialTheme.typography.bodyLarge
    val colors = OutlinedTextFieldDefaults.colors()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        modifier = modifier,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                label = label,
                colors = colors,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape
                    )
                }
            )
        }
    )
}
