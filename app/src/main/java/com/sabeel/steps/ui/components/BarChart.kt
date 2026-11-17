package com.sabeel.steps.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

data class BarChartEntry(
    val label: String,
    val value: Int,
)

/**
 * Original canvas bar chart: teal bars, dashed sand goal line. No chart library.
 * The drawing itself is hidden from TalkBack; the caller passes one text summary instead.
 *
 * The canvas is laid out left-to-right by the drawing API while the label Row is mirrored by RTL
 * layout, so in RTL the bars are mirrored explicitly: entry 0 sits on the right, exactly above its
 * label. Without this every Arabic weekday name would sit under the wrong day's bar.
 *
 * Light-theme contrast (WCAG 2.2 SC 1.4.11 needs >= 3:1 against the #FAF7F2 background):
 * goal line #8C6212 = 5.08:1; below-goal bars (#0F6B5C at 70% over the background = #569589) =
 * 3.24:1; the zero-day marker uses outline #79746C = 4.34:1. Dark-theme colours are already above
 * 9:1 and are left as designed.
 */
@Composable
fun BarChart(
    entries: List<BarChartEntry>,
    goal: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    showLabels: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val grow by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "bar-chart",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .clearAndSetSemantics { this.contentDescription = contentDescription },
        ) {
            if (entries.isEmpty()) return@Canvas
            val top = (max(entries.maxOf { it.value }, goal) * 1.15f).coerceAtLeast(1f)
            val goalY = size.height - (goal / top) * size.height
            drawLine(
                color = tertiary,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
            )
            val gap = if (entries.size > 14) 2.dp.toPx() else 6.dp.toPx()
            val barWidth = ((size.width - gap * (entries.size - 1)) / entries.size).coerceAtLeast(1f)
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            // Mirrored row order for RTL so each bar sits above the label rendered below it.
            val rtl = layoutDirection == LayoutDirection.Rtl
            entries.forEachIndexed { index, entry ->
                val x = if (rtl) {
                    size.width - (index + 1) * (barWidth + gap) + gap
                } else {
                    index * (barWidth + gap)
                }
                val ratio = (entry.value / top) * grow
                val barHeight = size.height * ratio
                if (barHeight <= 0.5f) {
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(x, size.height - 2.dp.toPx()),
                        size = Size(barWidth, 2.dp.toPx()),
                        cornerRadius = radius,
                    )
                } else {
                    drawRoundRect(
                        color = if (entry.value >= goal) primary else primary.copy(alpha = 0.7f),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = radius,
                    )
                }
            }
        }
        if (showLabels) {
            Row(modifier = Modifier.fillMaxWidth()) {
                entries.forEach { entry ->
                    Text(
                        text = entry.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
