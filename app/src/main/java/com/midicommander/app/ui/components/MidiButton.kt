package com.midicommander.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.midicommander.app.data.model.ControlConfig
import com.midicommander.app.data.model.MidiMessageType
import com.midicommander.app.ui.theme.MidiColors

// ─── MIDI Button ─────────────────────────────────────────────────────────────

@Composable
fun MidiButton(
    control: ControlConfig,
    editMode: Boolean,
    modifier: Modifier = Modifier,
    onPress: (ControlConfig) -> Unit,
    onLongPress: (ControlConfig) -> Unit
) {
    val accentColor = remember(control.colorHex) {
        try { Color(android.graphics.Color.parseColor(control.colorHex)) }
        catch (_: Exception) { MidiColors.PurpleGlow }
    }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 0.25f,
        animationSpec = tween(120),
        label = "btn_glow"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                // Glow border
                val strokePx = 2.dp.toPx()
                val glowColor = accentColor.copy(alpha = glowAlpha)
                drawRoundRect(
                    color = glowColor,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = strokePx)
                )
                // Background gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = if (pressed) 0.40f else 0.15f),
                            MidiColors.SurfaceCard.copy(alpha = 0.95f)
                        )
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .pointerInput(control, editMode) {
                detectTapGestures(
                    onPress = { offset ->
                        pressed = true
                        try { awaitRelease() } finally { pressed = false }
                    },
                    onTap = { if (!editMode) onPress(control) },
                    onLongPress = { onLongPress(control) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = control.label,
                color = MidiColors.TextPrimary,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildTypeLabel(control),
                color = accentColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                textAlign = TextAlign.Center
            )
        }
        // Edit mode indicator
        if (editMode) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .background(MidiColors.AmberWarm, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

private fun buildTypeLabel(control: ControlConfig): String = when (control.messageType) {
    MidiMessageType.PROGRAM_CHANGE -> "PC ${control.value}  CH${control.midiChannel}"
    MidiMessageType.CONTROL_CHANGE -> "CC ${control.value}  CH${control.midiChannel}"
    MidiMessageType.NOTE_ON        -> "Note ${control.value}  CH${control.midiChannel}"
    MidiMessageType.NOTE_OFF       -> "Off ${control.value}  CH${control.midiChannel}"
    MidiMessageType.SYSEX          -> "SysEx  CH${control.midiChannel}"
}
