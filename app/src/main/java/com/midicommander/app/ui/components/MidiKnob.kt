package com.midicommander.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.midicommander.app.data.model.ControlConfig
import com.midicommander.app.ui.theme.MidiColors
import kotlin.math.*

// ─── Rotary Knob ─────────────────────────────────────────────────────────────

@Composable
fun MidiKnob(
    control: ControlConfig,
    currentValue: Int,
    editMode: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
    onLongPress: (ControlConfig) -> Unit
) {
    val accentColor = remember(control.colorHex) {
        try { Color(android.graphics.Color.parseColor(control.colorHex)) }
        catch (_: Exception) { MidiColors.PurpleGlow }
    }

    val fraction = currentValue / 127f
    // Map 0–127 to -135° to +135° (270° sweep)
    val sweepDeg = 270f
    val startDeg = 135f
    val currentDeg by animateFloatAsState(
        targetValue = startDeg + fraction * sweepDeg,
        animationSpec = tween(80),
        label = "knob_angle"
    )

    var lastY by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .pointerInput(editMode, control) {
                    if (editMode) {
                        detectDragGestures { _, _ -> onLongPress(control) }
                    } else {
                        detectDragGestures(
                            onDragStart = { lastY = it.y },
                            onDrag = { change, _ ->
                                change.consume()
                                val delta = (lastY - change.position.y) / size.height
                                lastY = change.position.y
                                val newVal = (currentValue + (delta * 127).toInt()).coerceIn(0, 127)
                                onValueChange(newVal)
                            }
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val radius = size.minDimension / 2f - 4.dp.toPx()
                val trackStroke = 3.dp.toPx()
                val arcStroke = 4.dp.toPx()

                // Outer ring
                drawCircle(color = MidiColors.SurfaceCard, radius = radius + 4.dp.toPx())
                drawCircle(color = MidiColors.CardBorder, radius = radius + 4.dp.toPx(), style = Stroke(1.dp.toPx()))

                // Track arc (grey)
                drawArc(
                    color = MidiColors.TextMuted.copy(0.25f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = trackStroke, cap = StrokeCap.Round)
                )

                // Active arc
                drawArc(
                    brush = Brush.sweepGradient(listOf(accentColor.copy(0.5f), accentColor), center = Offset(cx, cy)),
                    startAngle = 135f,
                    sweepAngle = fraction * 270f,
                    useCenter = false,
                    style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                )

                // Pointer dot
                val angleRad = Math.toRadians(currentDeg.toDouble())
                val dotX = (cx + cos(angleRad) * (radius - arcStroke)).toFloat()
                val dotY = (cy + sin(angleRad) * (radius - arcStroke)).toFloat()
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(dotX, dotY))
                drawCircle(color = accentColor, radius = 2.dp.toPx(), center = Offset(dotX, dotY))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$currentValue",
            color = accentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = control.label,
            color = MidiColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = "CC${control.value}",
            color = MidiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp)
        )
    }
}
