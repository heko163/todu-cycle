package com.aiso.loopreminder.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aiso.loopreminder.data.firesOn
import com.aiso.loopreminder.ui.components.TaskCard
import com.aiso.loopreminder.ui.navigation.LoopBottomBar
import com.aiso.loopreminder.ui.theme.Cream
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Surface
import com.aiso.loopreminder.ui.theme.TextPrimary
import com.aiso.loopreminder.ui.theme.TextSecondary
import com.aiso.loopreminder.ui.util.isCompletedToday
import com.aiso.loopreminder.ui.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.LocalDate

private fun dateLabel(d: LocalDate): String {
    val dow = when (d.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"; DayOfWeek.TUESDAY -> "周二"; DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"; DayOfWeek.FRIDAY -> "周五"; DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"; else -> ""
    }
    return "${d.monthValue}月${d.dayOfMonth}日 $dow"
}

@Composable
fun CalendarScreen(navController: NavController, vm: TaskViewModel) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var selected by remember { mutableStateOf(today) }
    var monthDate by remember { mutableStateOf(LocalDate.of(today.year, today.month, 1)) }
    val daysInMonth = monthDate.lengthOfMonth()
    val startOffset = (monthDate.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val rows = (startOffset + daysInMonth + 6) / 7

    val changeMonth: (Int) -> Unit = { delta ->
        monthDate = monthDate.plusMonths(delta.toLong())
        // 切回"今天所在月"时自动选中今天，否则选中该月 1 号，保证下方列表总有内容。
        selected = if (monthDate.year == today.year && monthDate.month == today.month) today
                   else LocalDate.of(monthDate.year, monthDate.month, 1)
    }

    val selTasks = tasks.filter { it.firesOn(selected) }

    Scaffold(bottomBar = { LoopBottomBar(navController) }) { padding ->
        Column(
            Modifier.fillMaxSize().background(Cream).padding(padding)
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { changeMonth(-1) }) { Text("‹", fontSize = 28.sp, color = TextPrimary) }
                Text(
                    "${monthDate.year}年${monthDate.monthValue}月",
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                )
                TextButton(onClick = { changeMonth(1) }) { Text("›", fontSize = 28.sp, color = TextPrimary) }
            }
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height((rows * 52).dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = false
            ) {
                items(startOffset) { Box(Modifier.size(40.dp)) }
                items(daysInMonth) { i ->
                    val day = i + 1
                    val date = LocalDate.of(monthDate.year, monthDate.month, day)
                    val dots = tasks.count { it.firesOn(date) }
                    DayCell(day, date == today, dots, date == selected) { selected = date }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "${dateLabel(selected)}${if (selected == today) " · 今日" else ""} 待办 (${selTasks.size})",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            if (selTasks.isEmpty()) {
                Text("这一天没有提醒。", color = TextSecondary, fontSize = 14.sp)
            }
            selTasks.forEach { task ->
                TaskCard(
                    task = task,
                    onToggle = { vm.setCompleted(task, !task.isCompletedToday) },
                    onClick = { navController.navigate("detail/${task.id}") }
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DayCell(day: Int, isToday: Boolean, dotCount: Int, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isToday) Primary else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (isToday) Surface else TextPrimary
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .let { m -> if (isSelected && !isToday) m.border(1.5.dp, Primary, RoundedCornerShape(10.dp)) else m }
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day.toString(), color = fg, fontSize = 14.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
        if (dotCount > 0) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(6.dp).background(if (isToday) Surface else Primary, RoundedCornerShape(999.dp)))
        }
    }
}
