package com.aiso.loopreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.aiso.loopreminder.reminder.NotificationHelper
import com.aiso.loopreminder.ui.screens.CalendarScreen
import com.aiso.loopreminder.ui.screens.CategoryDetailScreen
import com.aiso.loopreminder.ui.screens.CategoryScreen
import com.aiso.loopreminder.ui.screens.NewReminderScreen
import com.aiso.loopreminder.ui.screens.TaskDetailScreen
import com.aiso.loopreminder.ui.screens.TodayScreen
import com.aiso.loopreminder.ui.theme.Cream
import com.aiso.loopreminder.ui.theme.LoopReminderTheme
import com.aiso.loopreminder.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoopReminderTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val vm: TaskViewModel = viewModel()

                // Make sure notification channels exist (no-op below API 26).
                LaunchedEffect(Unit) { NotificationHelper.createChannels(context) }

                // On Android 13+ the OS suppresses ALL notifications unless the user grants
                // POST_NOTIFICATIONS at runtime. Request it on first launch so reminders
                // actually appear in the status bar.
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* result is applied by the system; channels are already created */ }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // 主 tab 切换用纯淡入淡出（去掉默认 fade+scale 的缩放，避免"像新开窗口"的突兀感），200ms 干脆利落。
                val tabEnter = fadeIn(tween(200))
                val tabExit = fadeOut(tween(200))

                NavHost(
                    navController = navController,
                    startDestination = "today",
                    modifier = Modifier.fillMaxSize().background(Cream)
                ) {
                    composable(
                        "today",
                        enterTransition = { tabEnter },
                        exitTransition = { tabExit },
                        popEnterTransition = { tabEnter },
                        popExitTransition = { tabExit }
                    ) { TodayScreen(navController, vm) }
                    composable(
                        "categories",
                        enterTransition = { tabEnter },
                        exitTransition = { tabExit },
                        popEnterTransition = { tabEnter },
                        popExitTransition = { tabExit }
                    ) { CategoryScreen(navController, vm) }
                    composable("category/{catId}") { backStack ->
                        val id = backStack.arguments?.getString("catId")?.toLongOrNull() ?: 0L
                        CategoryDetailScreen(navController, vm, id)
                    }
                    composable("new") { NewReminderScreen(navController, vm) }
                    composable("edit/{taskId}") { backStack ->
                        val id = backStack.arguments?.getString("taskId")?.toLongOrNull()
                        NewReminderScreen(navController, vm, id)
                    }
                    composable(
                        "calendar",
                        enterTransition = { tabEnter },
                        exitTransition = { tabExit },
                        popEnterTransition = { tabEnter },
                        popExitTransition = { tabExit }
                    ) { CalendarScreen(navController, vm) }
                    composable("detail/{taskId}") { backStack ->
                        val id = backStack.arguments?.getString("taskId")?.toLongOrNull() ?: 0L
                        TaskDetailScreen(navController, vm, id)
                    }
                }
            }
        }
    }
}
