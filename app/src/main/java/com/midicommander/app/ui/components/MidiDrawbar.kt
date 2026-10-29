package com.midicommander.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.foundation.Canvas
import com.midicommander.app.data.model.ControlConfig
import com.midicommander.app.ui.theme.MidiColors

// ─── Drawbar / Slider ────────────────────────────────────────────────────────

@Composable
fun MidiDrawbar(
    control: ControlConfig,
    currentValue: Int,
    editMode: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
    onLongPress: (ControlConfig) -> Unit
) {
    val accentColor = remember(control.colorHex) {
        try { Color(android.graphics.Color.parseColor(control.colorHex)) }
        catch (_: Exception) { MidiColors.CyanAccent }
    }

    val fillFraction = currentValue / 127f

    val animatedFill by animateFloatAsState(fillFraction, animationSpec = tween(80), label = "fill")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Value label
        Text(
            text = "$currentValue",
            color = accentColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
        Spacer(Modifier.height(4.dp))

        // Drawbar track
        Box(
            modifier = Modifier
                .weight(1f)
                .width(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MidiColors.SurfaceCard)
                .pointerInput(editMode, control) {
                    if (editMode) {
                        detectDragGestures(onDragEnd = {}, onDrag = { _, _ -> onLongPress(control) })
                    } else {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newFraction = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            onValueChange((newFraction * 127).toInt().coerceIn(0, 127))
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Track background
                drawRoundRect(
                    color = MidiColors.CardBorder,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                )
                // Fill from bottom
                val fillH = size.height * animatedFill
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(0.9f), accentColor.copy(0.4f)),
                        startY = size.height - fillH,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, size.height - fillH),
                    size = androidx.compose.ui.geometry.Size(size.width, fillH)
                )
                // Notch lines
                val notchCount = 8
                for (i in 1..notchCount) {
                    val y = size.height * (i.toFloat() / (notchCount + 1))
                    drawLine(
                        color = MidiColors.TextMuted.copy(0.3f),
                        start = Offset(4f, y),
                        end = Offset(size.width - 4f, y),
                        strokeWidth = 1f
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        // Label
        Text(
            text = control.label,
            color = MidiColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = "CC${control.value}",
            color = MidiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            textAlign = TextAlign.Center
        )
    }
}
