package com.sabeel.steps.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sabeel.steps.R
import com.sabeel.steps.ui.theme.ringGradientColors

/**
 * Circular progress with an original teal-to-mint sweep gradient. Animates on value change and
 * exposes the percentage through semantics so TalkBack reads it as data, not as decoration.
 */
@Composable
fun ProgressRing(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 220.dp,
    strokeWidth: Dp = 16.dp,
    /** Shows the clay check badge on the ring once the daily goal is reached. */
    showCompleteBadge: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "progress-ring",
    )
    // outline keeps the unfilled track at 4.34:1 (light) / 5.8:1 (dark) instead of the old 1.11:1.
    val trackColor = MaterialTheme.colorScheme.outline
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gradient = ringGradientColors(dark)
    val completeColor = MaterialTheme.colorScheme.primary
    // A low dawn halo behind the ring; it is the one soft accent that carries the brand at a glance.
    val halo = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (dark) 0.22f else 0.5f)
    // One TalkBack node: the percentage description replaces the three nodes (canvas + two texts)
    // that were announced one after another before.
    Box(
        modifier = modifier
            .size(ringSize)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val haloRadius = size.minDimension * 0.64f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(halo, Color.Transparent),
                    center = center,
                    radius = haloRadius,
                ),
                radius = haloRadius,
                center = center,
            )
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.001f) {
                // At 100% a closing sweep gradient leaves a seam (and a lighter wedge) where its
                // start and end colours meet at 12 o'clock. A full circle is one solid colour.
                if (animated >= 0.999f) {
                    drawArc(
                        color = completeColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                } else {
                    // The sweep gradient's own seam sits at 3 o'clock; rotating the canvas by -90
                    // puts that seam at 12 o'clock where the arc begins, so the visible sweep runs
                    // dark-to-light with no hard colour step mid-ring.
                    rotate(degrees = -90f, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(gradient),
                            startAngle = 0f,
                            sweepAngle = 360f * animated,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                    // The round start cap samples the gradient just past its wrap and would show a
                    // lighter tip; a short arc in the start colour keeps the arc's first degrees clean.
                    if (animated > 0.04f) {
                        drawArc(
                            color = gradient.first(),
                            startAngle = -90f,
                            sweepAngle = 4f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
        if (showCompleteBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = 10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        content()
    }
}