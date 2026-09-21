package com.aiso.loopreminder.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiso.loopreminder.data.RecurrenceType
import com.aiso.loopreminder.data.Task
import com.aiso.loopreminder.ui.theme.Amber
import com.aiso.loopreminder.ui.theme.Border
import com.aiso.loopreminder.ui.theme.Danger
import com.aiso.loopreminder.ui.theme.Primary
import com.aiso.loopreminder.ui.theme.Success
import com.aiso.loopreminder.ui.theme.Surface
import com.aiso.loopreminder.ui.theme.TextPrimary
import com.aiso.loopreminder.ui.theme.TextSecondary
import com.aiso.loopreminder.ui.util.isCompletedToday
import com.aiso.loopreminder.ui.util.recurrenceSummary
import com.aiso.loopreminder.ui.util.isOverdue
import java.time.DayOfWeek

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Primary,
    trackColor: Color = Border
) {
    Canvas(modifier.size(56.dp)) {
        val stroke = 6.dp.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset(stroke / 2, stroke / 2)
        val rect = Size(diameter, diameter)
        drawArc(trackColor, 0f, 360f, false, topLeft = topLeft, size = rect, style = Stroke(stroke))
        drawArc(
            color, -90f, 360f * progress.coerceIn(0f, 1f), false,
            topLeft = topLeft, size = rect, style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    categoryColor: Color? = null,
    categoryName: String? = null
) {
    val done = task.isCompletedToday
    val overdue = task.isOverdue()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Success, checkmarkColor = Surface)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${task.recurrenceSummary()}  ${task.timeText}", color = TextSecondary, fontSize = 13.sp)
                    if (categoryName != null && categoryColor != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(6.dp).background(categoryColor, RoundedCornerShape(999.dp)))
                        Spacer(Modifier.width(4.dp))
                        Text(categoryName, color = categoryColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            val (label, c) = when {
                done -> "已完成" to Success
                overdue -> "逾期" to Danger
                else -> "待办" to Primary
            }
            androidx.compose.material3.Surface(
                color = c.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(label, color = c, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 15.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = Primary))
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        content = content
    )
}

@Composable
fun WeekdaySelector(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val order = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )
    val labels = mapOf(
        DayOfWeek.MONDAY to "一", DayOfWeek.TUESDAY to "二", DayOfWeek.WEDNESDAY to "三",
        DayOfWeek.THURSDAY to "四", DayOfWeek.FRIDAY to "五", DayOfWeek.SATURDAY to "六",
        DayOfWeek.SUNDAY to "日"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        order.forEach { d ->
            val isSel = selected.contains(d)
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSel) Primary else Surface)
                    .border(1.dp, if (isSel) Primary else Border, RoundedCornerShape(999.dp))
                    .clickable { onToggle(d) },
                contentAlignment = Alignment.Center
            ) {
                Text(labels[d]!!, color = if (isSel) Surface else TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun RecurrenceSelector(selected: RecurrenceType, onSelect: (RecurrenceType) -> Unit) {
    val items = RecurrenceType.values()
    Row(
        Modifier.fillMaxWidth()
            .background(Border, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { r ->
            val isSel = r == selected
            Box(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) Primary else Color.Transparent)
                    .clickable { onSelect(r) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(r.label, color = if (isSel) Surface else TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun IntervalSelector(minutes: Int, onMinutesChange: (Int) -> Unit) {
    val presets = listOf(30, 60, 120, 180)
    val label = if (minutes < 60) "${minutes} 分钟" else "${minutes / 60} 小时"
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("每", color = TextSecondary, fontSize = 14.sp)
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Border)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) { Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.width(4.dp))
        presets.forEach { p ->
            val isSel = p == minutes
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) Primary else Surface)
                    .border(1.dp, if (isSel) Primary else Border, RoundedCornerShape(8.dp))
                    .clickable { onMinutesChange(p) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    if (p < 60) "${p}分" else "${p / 60}时",
                    color = if (isSel) Surface else TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun StatusPill(label: String, color: Color) {
    androidx.compose.material3.Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(999.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, RoundedCornerShape(999.dp)))
            Spacer(Modifier.width(5.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
