package com.aiso.loopreminder.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aiso.loopreminder.data.CATEGORY_PALETTE
import com.aiso.loopreminder.data.Category
import com.aiso.loopreminder.ui.components.SectionCard
import com.aiso.loopreminder.ui.navigation.LoopBottomBar
import com.aiso.loopreminder.ui.theme.Cream
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Surface
import com.aiso.loopreminder.ui.theme.TextPrimary
import com.aiso.loopreminder.ui.theme.TextSecondary
import com.aiso.loopreminder.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class CatRow(
    val id: Long,
    val name: String,
    val color: Color,
    val count: Int,
    val isUncat: Boolean
)

@Composable
fun CategoryScreen(navController: NavController, vm: TaskViewModel) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        vm.exportBackup(uri)
        Toast.makeText(context, "已导出备份文件", Toast.LENGTH_SHORT).show()
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        vm.importBackup(uri) { imported, cats ->
            if (imported < 0) {
                Toast.makeText(context, "导入失败：文件无法读取或格式不正确", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "已导入 $imported 项待办、$cats 个分类", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var dialogName by remember { mutableStateOf("") }
    var dialogColor by remember { mutableStateOf(CATEGORY_PALETTE[0]) }

    fun openCreate() {
        editing = null
        dialogName = ""
        dialogColor = CATEGORY_PALETTE[0]
        showDialog = true
    }

    fun openEdit(cat: Category) {
        editing = cat
        dialogName = cat.name
        dialogColor = cat.color
        showDialog = true
    }

    // Each custom category is shown as a row; 未分类 is shown only if it has tasks.
    val rows = buildList {
        categories.forEach { cat ->
            val c = tasks.count { it.categoryId == cat.id }
            add(CatRow(cat.id, cat.name, Color(cat.color), c, false))
        }
        val uncat = tasks.count { it.categoryId == 0L }
        if (uncat > 0) add(CatRow(0L, "未分类", TextSecondary, uncat, true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办分类", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = TextPrimary)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("导出备份", color = TextPrimary) },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("loop-reminder-backup-${LocalDate.now()}.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入备份", color = TextPrimary) },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            )
        },
        bottomBar = { LoopBottomBar(navController) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(Cream).padding(padding)
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Text(
                "共 ${categories.size} 个分类 · 点开查看具体待办",
                fontSize = 14.sp, color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { openCreate() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Primary)
                Spacer(Modifier.width(6.dp))
                Text("新建分类", color = Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))

            if (rows.isEmpty()) {
                Text(
                    "还没有分类，点上方「新建分类」创建一个吧",
                    color = TextSecondary, fontSize = 14.sp
                )
            }

            rows.forEach { r ->
                CategoryRow(r, onClick = { navController.navigate("category/${r.id}") }) {
                    if (!r.isUncat) openEdit(categories.first { it.id == r.id })
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    if (editing == null) "新建分类" else "编辑分类",
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = dialogName,
                        onValueChange = { dialogName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("颜色", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CATEGORY_PALETTE.forEach { c ->
                            val isSel = c == dialogColor
                            Box(
                                Modifier.size(28.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(c))
                                    .border(
                                        if (isSel) 3.dp else 0.dp,
                                        if (isSel) Primary else Color.Transparent,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { dialogColor = c }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = dialogName.trim()
                    if (name.isBlank()) return@TextButton
                    val target = editing
                    if (target != null) {
                        vm.updateCategory(target.copy(name = name, color = dialogColor))
                    } else {
                        scope.launch { vm.addCategory(name, dialogColor) }
                    }
                    showDialog = false
                }) { Text("保存", color = Primary) }
            },
            dismissButton = {
                Row {
                    if (editing != null) {
                        TextButton(onClick = {
                            vm.deleteCategory(editing!!)
                            showDialog = false
                        }) { Text("删除", color = Color(0xFFEF4444)) }
                        Spacer(Modifier.width(4.dp))
                    }
                    TextButton(onClick = { showDialog = false }) { Text("取消", color = TextSecondary) }
                }
            }
        )
    }
}

@Composable
private fun CategoryRow(row: CatRow, onClick: () -> Unit, onEdit: () -> Unit) {
    SectionCard(
        Modifier.clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(999.dp)).background(row.color))
                Spacer(Modifier.width(10.dp))
                Text(row.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${row.count} 项", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Cream),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "查看", tint = Primary, modifier = Modifier.size(18.dp))
                }
                if (!row.isUncat) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑分类", tint = Primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
