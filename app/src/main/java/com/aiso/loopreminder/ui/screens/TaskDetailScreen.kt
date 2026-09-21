package com.aiso.loopreminder.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import com.aiso.loopreminder.ui.theme.Danger
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aiso.loopreminder.data.Task
import com.aiso.loopreminder.ui.components.SectionCard
import com.aiso.loopreminder.ui.components.StatusPill
import com.aiso.loopreminder.ui.components.SwitchRow
import com.aiso.loopreminder.ui.theme.Amber
import com.aiso.loopreminder.ui.theme.Border
import com.aiso.loopreminder.ui.theme.Cream
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Success
import com.aiso.loopreminder.ui.theme.Surface
import com.aiso.loopreminder.ui.theme.TextPrimary
import com.aiso.loopreminder.ui.theme.TextSecondary
import com.aiso.loopreminder.ui.util.isCompletedToday
import com.aiso.loopreminder.ui.util.recurrenceSummary
import com.aiso.loopreminder.ui.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun TaskDetailScreen(navController: NavController, vm: TaskViewModel, taskId: Long) {
    val taskFlow = remember(taskId) { vm.getTask(taskId) }
    val task by taskFlow.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务详情", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Text("‹", fontSize = 26.sp, color = TextPrimary) } },
                actions = {
                    if (task != null) {
                        IconButton(onClick = { navController.navigate("edit/${task!!.id}") }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑提醒", tint = Primary)
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除提醒", tint = Danger)
                    }
                }
            )
        },
        bottomBar = {
            if (task != null) {
                val t = task!!
                val done = t.isCompletedToday
                Box(Modifier.fillMaxWidth().background(Surface).padding(16.dp)) {
                    Row {
                        OutlinedButton(
                            onClick = { vm.snooze(t) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("推迟 1 小时", color = Primary) }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { vm.setCompleted(t, !done) },
                            modifier = Modifier.weight(2f),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (done) "标记未完成" else "标记完成", color = Surface) }
                    }
                }
            }
        }
    ) { padding ->
        if (task == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("加载中…", color = TextSecondary)
            }
            return@Scaffold
        }
        val t = task!!
        Column(
            Modifier.fillMaxSize().background(Cream).padding(padding)
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            SectionCard {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(t.recurrenceSummary(), Primary)
                        StatusPill(if (t.isCompletedToday) "已完成" else "进行中", if (t.isCompletedToday) Success else Amber)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(t.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("${t.recurrenceSummary()}  ${t.timeText}", color = TextSecondary, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionCard {
                InfoRow("重复", t.recurrenceSummary())
                Divider(color = Border, thickness = 1.dp)
                InfoRow("时间", t.timeText)
                Divider(color = Border, thickness = 1.dp)
                InfoRow("提前提醒", if (t.advanceNoticeMinutes == 0) "不提前" else "提前 ${t.advanceNoticeMinutes} 分钟")
                Divider(color = Border, thickness = 1.dp)
                InfoRow("提醒方式", t.reminderMode)
                Divider(color = Border, thickness = 1.dp)
                val cat = categories.firstOrNull { it.id == t.categoryId }
                InfoRow(
                    "分类",
                    if (cat != null) cat.name else "未分类",
                    leadingColor = if (cat != null) Color(cat.color) else TextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionCard {
                SwitchRow("未完成时持续提醒", "未标记完成则每 ${t.escalateIntervalMinutes} 分钟重复提醒，直到完成", t.escalateEnabled) {}
                Divider(color = Border, thickness = 1.dp)
                SwitchRow("通知栏常驻", "提醒存续期间常驻通知栏，标记完成后移除", t.persistentNotification) {}
            }

            if (t.notes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                SectionCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("备注", color = TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(t.notes, color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionCard {
                Column(Modifier.padding(16.dp)) {
                    Text("最近 7 天打卡", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val weekChars = mapOf(
                            DayOfWeek.MONDAY to "一", DayOfWeek.TUESDAY to "二", DayOfWeek.WEDNESDAY to "三",
                            DayOfWeek.THURSDAY to "四", DayOfWeek.FRIDAY to "五", DayOfWeek.SATURDAY to "六",
                            DayOfWeek.SUNDAY to "日"
                        )
                        val last7 = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                        last7.forEach { date ->
                            val done = t.lastCompletedDate == date.toString()
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(999.dp))
                                    .background(if (done) Success else Border),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(weekChars[date.dayOfWeek]!!, color = if (done) Surface else TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("仅记录最近一次完成日期（v1 简化）", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showDeleteDialog && task != null) {
        val t = task!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除提醒", color = TextPrimary) },
            text = { Text("确定删除「${t.title}」？删除后不再提醒，且历史记录一并清除。", color = TextSecondary) },
            confirmButton = {
                Text(
                    "删除",
                    color = Danger,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            vm.deleteTask(t)
                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                        .padding(12.dp)
                )
            },
            dismissButton = {
                Text(
                    "取消",
                    color = TextSecondary,
                    modifier = Modifier.clickable { showDeleteDialog = false }.padding(12.dp)
                )
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, leadingColor: Color? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingColor != null) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(leadingColor))
                Spacer(Modifier.width(6.dp))
            }
            Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
