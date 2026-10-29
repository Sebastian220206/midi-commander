package com.midicommander.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.midicommander.app.data.model.ControlConfig
import com.midicommander.app.data.model.ControlType
import com.midicommander.app.data.model.MidiMessageType
import com.midicommander.app.ui.theme.MidiColors

// ─── Control Config Dialog ────────────────────────────────────────────────────

@Composable
fun ControlConfigDialog(
    control: ControlConfig,
    onDismiss: () -> Unit,
    onSave: (ControlConfig) -> Unit,
    onReset: (ControlConfig) -> Unit,
    onStartMidiLearn: (String) -> Unit
) {
    var label     by remember { mutableStateOf(control.label) }
    var channel   by remember { mutableStateOf(control.midiChannel) }
    var msgType   by remember { mutableStateOf(control.messageType) }
    var value     by remember { mutableStateOf(control.value) }
    var secValue  by remember { mutableStateOf(control.secondaryValue) }
    var colorHex  by remember { mutableStateOf(control.colorHex) }
    var sysEx     by remember { mutableStateOf(control.sysExData) }
    var isToggle  by remember { mutableStateOf(control.isToggle) }
    var ctrlType  by remember { mutableStateOf(control.controlType) }
    var cableId   by remember { mutableStateOf(control.cableId) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MidiColors.SurfaceDark,
            border = BorderStroke(1.dp, MidiColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Text(
                    "Configure Control",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MidiColors.PurpleLight,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                // Control Type
                SectionLabel("Control Type")
                SegmentedRow(
                    options = ControlType.entries.map { it.name },
                    selected = ctrlType.ordinal,
                    onSelect = { ctrlType = ControlType.entries[it] }
                )

                // Label
                SectionLabel("Label")
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("Button Name", color = MidiColors.TextMuted) },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // MIDI Channel
                SectionLabel("MIDI Channel  $channel")
                Slider(
                    value = channel.toFloat(),
                    onValueChange = { channel = it.toInt() },
                    valueRange = 1f..16f,
                    steps = 14,
                    colors = sliderColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Message Type
                SectionLabel("Message Type")
                SegmentedRow(
                    options = MidiMessageType.entries.map { it.label },
                    selected = msgType.ordinal,
                    onSelect = { msgType = MidiMessageType.entries[it] }
                )

                // Value (primary)
                when (msgType) {
                    MidiMessageType.SYSEX -> {
                        SectionLabel("SysEx Data (hex)")
                        OutlinedTextField(
                            value = sysEx,
                            onValueChange = { sysEx = it },
                            placeholder = { Text("F0 41 10 ... F7", color = MidiColors.TextMuted) },
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        val vLabel = when (msgType) {
                            MidiMessageType.PROGRAM_CHANGE -> "Program"
                            MidiMessageType.CONTROL_CHANGE -> "CC Number"
                            MidiMessageType.NOTE_ON, MidiMessageType.NOTE_OFF -> "Note"
                            else -> "Value"
                        }
                        SectionLabel("$vLabel  $value")
                        Slider(
                            value = value.toFloat(),
                            onValueChange = { value = it.toInt() },
                            valueRange = 0f..127f,
                            colors = sliderColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Secondary value (velocity for notes, value for CC)
                        if (msgType == MidiMessageType.NOTE_ON || msgType == MidiMessageType.CONTROL_CHANGE) {
                            val secLabel = if (msgType == MidiMessageType.NOTE_ON) "Velocity  $secValue" else "CC Value  $secValue"
                            SectionLabel(secLabel)
                            Slider(
                                value = secValue.toFloat(),
                                onValueChange = { secValue = it.toInt() },
                                valueRange = 0f..127f,
                                colors = sliderColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Cable ID
                SectionLabel("Port / Cable  $cableId")
                Slider(
                    value = cableId.toFloat(),
                    onValueChange = { cableId = it.toInt() },
                    valueRange = 0f..7f,
                    steps = 6,
                    colors = sliderColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isToggle,
                        onCheckedChange = { isToggle = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MidiColors.PurpleGlow)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Toggle mode", color = MidiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall)
                }

                // Color Picker
                SectionLabel("Button Color")
                ColorPalettePicker(selected = colorHex, onSelect = { colorHex = it })

                // MIDI Learn
                OutlinedButton(
                    onClick = { onStartMidiLearn(control.id); onDismiss() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MidiColors.CyanAccent),
                    border = BorderStroke(1.dp, MidiColors.CyanAccent),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🎛  MIDI Learn") }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReset(control); onDismiss() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MidiColors.RedAlert),
                        border = BorderStroke(1.dp, MidiColors.RedAlert),
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset") }

                    Button(
                        onClick = {
                            onSave(
                                control.copy(
                                    label = label.ifBlank { "—" },
                                    midiChannel = channel,
                                    messageType = msgType,
                                    value = value,
                                    secondaryValue = secValue,
                                    colorHex = colorHex,
                                    sysExData = sysEx,
                                    isToggle = isToggle,
                                    controlType = ctrlType,
                                    cableId = cableId
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MidiColors.PurpleGlow),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = MidiColors.TextSecondary,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun SegmentedRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, opt ->
            val bg = if (i == selected) MidiColors.PurpleGlow else MidiColors.SurfaceCard
            val tc = if (i == selected) Color.White else MidiColors.TextSecondary
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .border(1.dp, if (i == selected) MidiColors.PurpleLight else MidiColors.CardBorder, RoundedCornerShape(8.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(opt, color = tc, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun ColorPalettePicker(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MidiColors.ButtonColors.forEach { color ->
            val hex = "#%06X".format(0xFFFFFF and color.toArgb())
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (isSelected) 2.dp else 0.dp, Color.White, CircleShape)
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MidiColors.PurpleGlow,
    unfocusedBorderColor = MidiColors.CardBorder,
    focusedTextColor = MidiColors.TextPrimary,
    unfocusedTextColor = MidiColors.TextPrimary,
    cursorColor = MidiColors.PurpleGlow
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = MidiColors.PurpleGlow,
    activeTrackColor = MidiColors.PurpleGlow,
    inactiveTrackColor = MidiColors.CardBorder
)
