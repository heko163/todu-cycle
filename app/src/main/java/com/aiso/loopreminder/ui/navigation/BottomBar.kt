package com.aiso.loopreminder.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Surface as Surf
import com.aiso.loopreminder.ui.theme.TextSecondary

sealed class Tab(val route: String, val label: String) {
    object Todos : Tab("today", "待办")
    object Categories : Tab("categories", "分类")
    object Calendar : Tab("calendar", "日历")
}

@Composable
fun LoopBottomBar(navController: NavController) {
    val items = listOf(Tab.Todos, Tab.Categories, Tab.Calendar)
    val current = navController.currentDestination

    Surface(shadowElevation = 4.dp, color = Surf) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { tab ->
                val sel = current?.hierarchy?.any { it.route == tab.route } == true
                Column(
                    Modifier.clickable { navController.navigate(tab.route) { launchSingleTop = true; restoreState = true } },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab.label,
                        color = if (sel) Primary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal
                    )
                    if (sel) {
                        Box(
                            Modifier.padding(top = 4.dp).size(width = 20.dp, height = 3.dp)
                                .background(Primary, RoundedCornerShape(999.dp))
                        )
                    }
                }
            }
        }
    }
}
