package com.aiso.loopreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aiso.loopreminder.data.firesOn
import com.aiso.loopreminder.data.isOverdueUnfinished
import com.aiso.loopreminder.ui.components.ProgressRing
import com.aiso.loopreminder.ui.components.SectionCard
import com.aiso.loopreminder.ui.components.TaskCard
import com.aiso.loopreminder.ui.navigation.LoopBottomBar
import com.aiso.loopreminder.ui.theme.Cream
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Surface
import com.aiso.loopreminder.ui.theme.TextPrimary
import com.aiso.loopreminder.ui.theme.TextSecondary
import com.aiso.loopreminder.ui.util.isCompletedToday
import com.aiso.loopreminder.ui.viewmodel.TaskViewModel
import java.time.LocalDate

@Composable
fun TodayScreen(navController: NavController, vm: TaskViewModel) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val catMap = categories.associateBy { it.id }
    val today = LocalDate.now()
    val todayTasks = tasks.filter { it.firesOn(today) || it.isOverdueUnfinished(today) }
        // Overdue (not firing today but still pending) items go to the top, then by time.
        .sortedWith(compareBy({ !it.isOverdueUnfinished(today) }, { it.timeMinuteOfDay }))
    val total = todayTasks.size
    val doneToday = todayTasks.count { it.isCompletedToday }
    val pending = (total - doneToday).coerceAtLeast(0)
    val progress = if (total == 0) 0f else doneToday.toFloat() / total

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("new") }, containerColor = Primary) {
                Text("+", color = Surface, fontSize = 26.sp)
            }
        },
        bottomBar = { LoopBottomBar(navController) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(Cream).padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            Text("今日待办", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("今日 $total 项提醒 · 已完成 $doneToday 项", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(16.dp))

            SectionCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("$total 项", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text("$pending 项未完成", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (todayTasks.isEmpty()) {
                Text("今天没有需要提醒的待办，享受轻松的一天吧", color = TextSecondary, fontSize = 14.sp)
            }
            todayTasks.forEach { task ->
                val cat = catMap[task.categoryId]
                TaskCard(
                    task = task,
                    onToggle = { vm.setCompleted(task, !task.isCompletedToday) },
                    onClick = { navController.navigate("detail/${task.id}") },
                    categoryColor = cat?.color?.let { Color(it) },
                    categoryName = cat?.name
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
