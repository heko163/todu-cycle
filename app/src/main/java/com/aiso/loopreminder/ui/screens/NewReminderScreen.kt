package com.aiso.loopreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aiso.loopreminder.data.CATEGORY_PALETTE
import com.aiso.loopreminder.data.Category
import com.aiso.loopreminder.data.RecurrenceType
import com.aiso.loopreminder.data.Task
import com.aiso.loopreminder.ui.components.IntervalSelector
import com.aiso.loopreminder.ui.components.RecurrenceSelector
import com.aiso.loopreminder.ui.components.SectionCard
import com.aiso.loopreminder.ui.components.SwitchRow
import com.aiso.loopreminder.ui.components.WeekdaySelector
import com.aiso.loopreminder.ui.theme.Border
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Surface
import com.aiso.loopreminder.ui.theme.TextPrimary
import com.aiso.loopreminder.ui.theme.TextSecondary
import com.aiso.loopreminder.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.DayOfWeek as JDayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private fun fmt(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

/** Actual number of days in [month]. Uses a leap year so February offers 29 (clamped later for non-leap years). */
private fun daysInMonth(month: Int): Int = YearMonth.of(2000, month).lengthOfMonth()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReminderScreen(navController: NavController, vm: TaskViewModel, taskId: Long? = null) {
    val editing = taskId != null
    // FIX: keep the flow instance stable across recompositions. Previously getTask() was
    // re-invoked on every recomposition (e.g. on each keystroke), which rebuilt the StateFlow
    // and momentarily reset `existing` to null — so saving during that window ran addTask()
    // and created a duplicate instead of updating. Remembering by taskId keeps the same flow.
    val existing by remember(taskId) {
        if (editing) vm.getTask(taskId!!) else flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)

    var initialized by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(RecurrenceType.DAILY) }
    var timeMin by remember { mutableStateOf(12 * 60 + 30) }
    var repeatDays by remember { mutableStateOf(setOf(JDayOfWeek.MONDAY, JDayOfWeek.WEDNESDAY, JDayOfWeek.FRIDAY)) }
    var advance by remember { mutableStateOf(0) }
    var escalate by remember { mutableStateOf(false) }
    var escalateMin by remember { mutableStateOf(60) }
    var persistent by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var monthDay by remember { mutableStateOf(minOf(LocalDate.now().dayOfMonth, 31)) }
    var yearMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var yearDay by remember { mutableStateOf(minOf(LocalDate.now().dayOfMonth, daysInMonth(LocalDate.now().monthValue))) }
    var showTime by remember { mutableStateOf(false) }
    var categoryId by remember { mutableStateOf(0L) }
    var showCatDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatColor by remember { mutableStateOf(CATEGORY_PALETTE[0]) }
    val scope = rememberCoroutineScope()
    val categories by vm.categories.collectAsStateWithLifecycle()

    // Time picker is seeded from the task being edited (rememberTimePickerState re-keys on its initial args).
    val tpInitialMinute = existing?.timeMinuteOfDay ?: (12 * 60 + 30)
    val tpState = rememberTimePickerState(initialHour = tpInitialMinute / 60, initialMinute = tpInitialMinute % 60)

    // Pre-fill all fields once from the existing task (edit mode only).
    LaunchedEffect(existing) {
        if (existing != null && !initialized) {
            val t = existing!!
            title = t.title
            recurrence = t.recurrence
            timeMin = t.timeMinuteOfDay
            repeatDays = t.repeatDays
            advance = t.advanceNoticeMinutes
            escalate = t.escalateEnabled
            escalateMin = t.escalateIntervalMinutes
            persistent = t.persistentNotification
            notes = t.notes
            monthDay = t.monthDay.coerceIn(1, 31)
            yearMonth = t.yearMonth.coerceIn(1, 12)
            yearDay = t.yearDay.coerceIn(1, daysInMonth(yearMonth))
            categoryId = t.categoryId
            initialized = true
        }
    }

    val advancePresets = listOf(0, 5, 15, 30)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "编辑提醒" else "新建提醒", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Text("‹", fontSize = 26.sp, color = TextPrimary) } }
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(Surface).padding(16.dp)) {
                Button(
                    onClick = {
                        if (title.isBlank()) return@Button
                        // Use the route param `taskId` (not `existing != null`) as the source of
                        // truth for whether we're editing — so a late-loading flow can never make
                        // us accidentally insert a duplicate.
                        val base = existing
                        val task = Task(
                            id = if (editing) taskId!! else 0,
                            title = title.trim(),
                            recurrence = recurrence,
                            timeMinuteOfDay = timeMin,
                            repeatDays = if (recurrence == RecurrenceType.WEEKLY) repeatDays else emptySet(),
                            advanceNoticeMinutes = advance,
                            escalateEnabled = escalate,
                            escalateIntervalMinutes = escalateMin,
                            persistentNotification = persistent,
                            monthDay = if (recurrence == RecurrenceType.MONTHLY) monthDay.coerceIn(1, 31) else 1,
                            yearMonth = if (recurrence == RecurrenceType.YEARLY) yearMonth.coerceIn(1, 12) else 1,
                            yearDay = if (recurrence == RecurrenceType.YEARLY) yearDay.coerceIn(1, daysInMonth(yearMonth)) else 1,
                            notes = notes.trim(),
                            categoryId = categoryId,
                            createdAt = base?.createdAt ?: System.currentTimeMillis(),
                            lastCompletedDate = base?.lastCompletedDate,
                            active = base?.active ?: true
                        )
                        if (editing) vm.updateTask(task) else vm.addTask(task)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("保存", color = Surface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            SectionCard {
                Row(
                    Modifier.fillMaxWidth().clickable { showTime = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("时间", color = TextPrimary, fontSize = 15.sp)
                    Text(fmt(timeMin), color = Primary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("提前", color = TextSecondary, fontSize = 14.sp)
                    advancePresets.forEach { a ->
                        val isSel = a == advance
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Primary else Surface)
                                .border(1.dp, if (isSel) Primary else Border, RoundedCornerShape(8.dp))
                                .clickable { advance = a }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text(if (a == 0) "不提前" else "提前${a}分", color = if (isSel) Surface else TextSecondary, fontSize = 13.sp) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("重复", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            RecurrenceSelector(recurrence) { recurrence = it }
            if (recurrence == RecurrenceType.WEEKLY) {
                Spacer(Modifier.height(12.dp))
                WeekdaySelector(repeatDays) { d ->
                    repeatDays = if (repeatDays.contains(d)) repeatDays - d else repeatDays + d
                }
            }

            if (recurrence == RecurrenceType.MONTHLY) {
                Spacer(Modifier.height(12.dp))
                SectionCard {
                    Text("每月几日", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(16.dp))
                    Text(
                        "可选 1–31 日；若当月无该日（如 2 月、小月），则在该月最后一天提醒",
                        color = TextSecondary, fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..31).forEach { d ->
                            SelectableChip("${d}日", d == monthDay) { monthDay = d }
                        }
                    }
                }
            }

            if (recurrence == RecurrenceType.YEARLY) {
                Spacer(Modifier.height(12.dp))
                SectionCard {
                    Text("每年几月", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(16.dp))
                    FlowRow(
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val monthNames = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
                        monthNames.forEachIndexed { idx, name ->
                            val m = idx + 1
                            SelectableChip(name, m == yearMonth) {
                                yearMonth = m
                                val maxD = daysInMonth(m)
                                if (yearDay > maxD) yearDay = maxD
                            }
                        }
                    }
                    Text("几日", color = TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp))
                    val maxDay = daysInMonth(yearMonth)
                    FlowRow(
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..maxDay).forEach { d ->
                            SelectableChip("${d}日", d == yearDay) { yearDay = d }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionCard {
                SwitchRow(
                    "未完成时持续提醒",
                    "未标记完成则按间隔反复提醒，直到完成",
                    escalate
                ) { escalate = it }
                if (escalate) {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        IntervalSelector(escalateMin) { escalateMin = it }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionCard {
                SwitchRow("通知栏常驻", "提醒存续期间常驻通知栏，标记完成后移除", persistent) { persistent = it }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(16.dp))
            Text("分类", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            SectionCard {
                FlowRow(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip("未分类", TextSecondary, categoryId == 0L) { categoryId = 0L }
                    categories.forEach { c ->
                        CategoryChip(c.name, Color(c.color), categoryId == c.id) { categoryId = c.id }
                    }
                    // 「＋ 新建」creates a new category inline
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Surface)
                            .border(1.dp, Primary, RoundedCornerShape(8.dp))
                            .clickable { showCatDialog = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("＋ 新建", color = Primary, fontSize = 13.sp) }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTime) {
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    showTime = false
                    timeMin = tpState.hour * 60 + tpState.minute
                }) { Text("确定", color = Primary) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消", color = TextSecondary) } },
            text = { TimePicker(state = tpState) }
        )
    }

    if (showCatDialog) {
        AlertDialog(
            onDismissRequest = { showCatDialog = false },
            title = { Text("新建分类", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("颜色", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CATEGORY_PALETTE.forEach { c ->
                            val isSel = c == newCatColor
                            Box(
                                Modifier.size(28.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(c))
                                    .border(
                                        if (isSel) 3.dp else 0.dp,
                                        if (isSel) Primary else Color.Transparent,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { newCatColor = c }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCatName.trim()
                    if (name.isBlank()) return@TextButton
                    scope.launch {
                        val id = vm.addCategory(name, newCatColor)
                        categoryId = id
                    }
                    newCatName = ""
                    newCatColor = CATEGORY_PALETTE[0]
                    showCatDialog = false
                }) { Text("保存", color = Primary) }
            },
            dismissButton = { TextButton(onClick = { showCatDialog = false }) { Text("取消", color = TextSecondary) } }
        )
    }
}

@Composable
private fun CategoryChip(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else Surface)
            .border(1.dp, if (selected) color else Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(name, color = if (selected) color else TextSecondary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
        }
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) Primary else Surface)
            .border(1.dp, if (selected) Primary else Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { Text(text, color = if (selected) Surface else TextSecondary, fontSize = 13.sp) }
}
