package com.aiso.loopreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.aiso.loopreminder.ui.components.SectionCard
import com.aiso.loopreminder.ui.components.TaskCard
import com.aiso.loopreminder.ui.theme.Cream
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Surface
import com.aiso.loopreminder.ui.theme.TextPrimary
import com.aiso.loopreminder.ui.theme.TextSecondary
import com.aiso.loopreminder.ui.util.isCompletedToday
import com.aiso.loopreminder.ui.viewmodel.TaskViewModel

@Composable
fun CategoryDetailScreen(navController: NavController, vm: TaskViewModel, catId: Long) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val cat = if (catId == 0L) null else categories.firstOrNull { it.id == catId }
    val color = cat?.color?.let { Color(it) } ?: TextSecondary
    val title = cat?.name ?: "未分类"
    val catTasks = tasks.filter { it.categoryId == catId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("‹", fontSize = 26.sp, color = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(Cream).padding(padding)
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            SectionCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(999.dp)).background(color))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text("共 ${catTasks.size} 项待办", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (catTasks.isEmpty()) {
                Text("这个分类还没有待办，去「待办」页右下角 + 添加一个吧", color = TextSecondary, fontSize = 14.sp)
            }
            catTasks.forEach { task ->
                TaskCard(
                    task = task,
                    onToggle = { vm.setCompleted(task, !task.isCompletedToday) },
                    onClick = { navController.navigate("detail/${task.id}") }
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
