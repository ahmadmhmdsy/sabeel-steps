package com.sabeel.steps.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sabeel.steps.domain.RoutePoint

/**
 * Draws the recorded GPS route on our own canvas. Deliberately NOT a map: no tiles, no API key,
 * no network, just the shape of the walk normalised into the card.
 */
@Composable
fun RouteSketch(
    route: List<RoutePoint>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    sketchHeight: Dp = 180.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    endColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(sketchHeight)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(sketchHeight)) {
            if (route.size < 2) return@Canvas
            val minLat = route.minOf { it.lat }
            val maxLat = route.maxOf { it.lat }
            val minLng = route.minOf { it.lng }
            val maxLng = route.maxOf { it.lng }
            val padding = 14.dp.toPx()
            val drawWidth = (size.width - padding * 2f).coerceAtLeast(1f)
            val drawHeight = (size.height - padding * 2f).coerceAtLeast(1f)
            val latSpan = (maxLat - minLat).takeIf { it > 0.0000001 } ?: 1.0
            val lngSpan = (maxLng - minLng).takeIf { it > 0.0000001 } ?: 1.0
            fun point(p: RoutePoint): Offset {
                val nx = ((p.lng - minLng) / lngSpan).toFloat()
                val ny = ((p.lat - minLat) / latSpan).toFloat()
                return Offset(
                    x = padding + nx * drawWidth,
                    y = padding + (1f - ny) * drawHeight,
                )
            }
            val path = Path()
            route.forEachIndexed { index, point ->
                val offset = point(point)
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            val start = point(route.first())
            val end = point(route.last())
            drawCircle(color = lineColor, radius = 5.dp.toPx(), center = start)
            drawCircle(color = endColor, radius = 7.dp.toPx(), center = end)
        }
    }
}
