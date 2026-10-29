package com.midicommander.app.ui.components

import android.media.midi.MidiDeviceInfo
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.midicommander.app.ui.theme.MidiColors

// ─── Device Picker Dialog ─────────────────────────────────────────────────────

@Composable
fun DevicePickerDialog(
    devices: List<MidiDeviceInfo>,
    onDismiss: () -> Unit,
    onSelect: (MidiDeviceInfo, Int) -> Unit   // deviceInfo, portIndex
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MidiColors.SurfaceDark,
            border = BorderStroke(1.dp, MidiColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Usb, contentDescription = null,
                        tint = MidiColors.CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Select MIDI Device",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MidiColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ))
                }
                Spacer(Modifier.height(12.dp))

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.UsbOff, contentDescription = null,
                                tint = MidiColors.TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No MIDI devices found",
                                color = MidiColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text("Connect a USB MIDI device and try again.",
                                color = MidiColors.TextMuted,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(devices) { info ->
                            DeviceItem(info = info, onSelect = { portIdx -> onSelect(info, portIdx) })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = MidiColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(info: MidiDeviceInfo, onSelect: (Int) -> Unit) {
    val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
        ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
        ?: "Unknown Device"
    val manufacturer = info.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) ?: ""
    val inputPorts = info.inputPortCount
    val outputPorts = info.outputPortCount

    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = MidiColors.SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MidiColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (inputPorts <= 1) onSelect(0)
                        else expanded = !expanded
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Piano, contentDescription = null,
                    tint = MidiColors.PurpleLight, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = MidiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    if (manufacturer.isNotBlank()) {
                        Text(manufacturer, color = MidiColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text("In: $inputPorts  Out: $outputPorts",
                        color = MidiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                }
                if (inputPorts > 1) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MidiColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MidiColors.CyanAccent, modifier = Modifier.size(18.dp))
                }
            }

            // Port selection when multi-port
            if (expanded && inputPorts > 1) {
                HorizontalDivider(color = MidiColors.CardBorder)
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Port:", color = MidiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall)
                    for (p in 0 until inputPorts) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(p) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Cable, contentDescription = null,
                                tint = MidiColors.CyanAccent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Port $p", color = MidiColors.TextPrimary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
