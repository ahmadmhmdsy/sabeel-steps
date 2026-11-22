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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sabeel.steps.R
import com.sabeel.steps.ui.theme.ringGradientColors

/**
 * Circular progress with an original teal-to-mint sweep gradient. Animates on value change and
 * exposes the percentage through semantics so TalkBack reads it as data, not as decoration.
 *
 * When [countingEnabled] and [onToggleCounting] are both supplied the whole ring becomes the
 * counting switch (plan 5.6): one merged TalkBack node that still reports the step progress, adds
 * the switch role plus the on/off [stateDescriptionOn]/[stateDescriptionOff], and announces the
 * action with [toggleOnLabel]/[toggleOffLabel]. The existing content description is kept, so the
 * ring is never only a switch with no number. Callers that pass neither parameter keep the exact
 * previous behaviour.
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
    /** Counting intent of the ring switch; null keeps the ring non-interactive. */
    countingEnabled: Boolean? = null,
    /** Dispatched when the ring is tapped; null keeps the ring non-interactive. */
    onToggleCounting: (() -> Unit)? = null,
    /** TalkBack onClickLabel while counting is on, normally "Stop counting". */
    toggleOnLabel: String? = null,
    /** TalkBack onClickLabel while counting is off, normally "Start counting". */
    toggleOffLabel: String? = null,
    /** TalkBack stateDescription while counting is on, normally "Counting on". */
    stateDescriptionOn: String? = null,
    /** TalkBack stateDescription while counting is off, normally "Counting off". */
    stateDescriptionOff: String? = null,
    /**
     * Mutes the track and the progress arc (the off state is a deliberate user choice, not an
     * error). The number inside the ring stays at full contrast so it can still be read.
     */
    dimmed: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "progress-ring",
    )
    // outline keeps the unfilled track at 4.34:1 (light) / 5.8:1 (dark) instead of the old 1.11:1.
    val idleTrack = MaterialTheme.colorScheme.outline
    val trackColor = if (dimmed) idleTrack.copy(alpha = 0.45f) else idleTrack
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gradient = ringGradientColors(dark).let { colors ->
        if (dimmed) colors.map { it.copy(alpha = 0.5f) } else colors
    }
    val completeColor = MaterialTheme.colorScheme.primary.let {
        if (dimmed) it.copy(alpha = 0.5f) else it
    }
    // A low dawn halo behind the ring; it is the one soft accent that carries the brand at a glance.
    val halo = MaterialTheme.colorScheme.tertiaryContainer
        .copy(alpha = if (dark) 0.22f else 0.5f)
        .let { if (dimmed) it.copy(alpha = it.alpha * 0.5f) else it }

    // Local copies: stable vals so the nullable toggle contract smart-casts inside the modifier
    // expression and inside the click lambda without force-unwraps.
    val toggleValue = countingEnabled
    val toggleAction = onToggleCounting
    val ringModifier = modifier.size(ringSize).then(
        if (toggleValue != null && toggleAction != null) {
            Modifier
                .toggleable(
                    value = toggleValue,
                    role = Role.Switch,
                    onValueChange = { toggleAction() },
                )
                .semantics(mergeDescendants = true) {
                    // The step progress is still the ring's content description; the switch state
                    // and its action label are added beside it instead of replacing it.
                    this.contentDescription = contentDescription
                    if (toggleValue) {
                        val onState = stateDescriptionOn
                        if (!onState.isNullOrBlank()) this.stateDescription = onState
                        // toggleable() already exposes the switch action; replace its default label
                        // with the caller's intent so TalkBack says "Stop counting", not "Toggle".
                        this.onClick(label = toggleOnLabel) { toggleAction(); true }
                    } else {
                        val offState = stateDescriptionOff
                        if (!offState.isNullOrBlank()) this.stateDescription = offState
                        this.onClick(label = toggleOffLabel) { toggleAction(); true }
                    }
                }
        } else {
            // One TalkBack node: the percentage description replaces the three nodes (canvas + two
            // texts) that were announced one after another before.
            Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
        },
    )

    Box(modifier = ringModifier, contentAlignment = Alignment.Center) {
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