package com.midicommander.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.midicommander.app.data.model.LogDirection
import com.midicommander.app.data.model.MidiLogEntry
import com.midicommander.app.data.model.MidiMessageType
import com.midicommander.app.ui.theme.MidiColors
import java.text.SimpleDateFormat
import java.util.*

// ─── MIDI Monitor Panel ───────────────────────────────────────────────────────

@Composable
fun MidiMonitorPanel(
    entries: List<MidiLogEntry>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    var filterChannel by remember { mutableStateOf(0) }  // 0 = all
    var filterType by remember { mutableStateOf<MidiMessageType?>(null) }

    val filtered = remember(entries, filterChannel, filterType) {
        entries.filter { e ->
            (filterChannel == 0 || e.channel == filterChannel) &&
            (filterType == null || e.messageType == filterType)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MidiColors.SurfaceDark,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(1.dp, MidiColors.CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MIDI Monitor",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MidiColors.CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                )
                Row {
                    Icon(Icons.Default.Circle,
                        contentDescription = null,
                        tint = MidiColors.GreenActive,
                        modifier = Modifier.size(10.dp).align(Alignment.CenterVertically))
                    Spacer(Modifier.width(4.dp))
                    Text("${filtered.size}", color = MidiColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MidiColors.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Filters row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Channel filter
                FilterChip(label = "All Ch", selected = filterChannel == 0,
                    onClick = { filterChannel = 0 })
                for (ch in listOf(1, 2, 3, 10, 16)) {
                    FilterChip(label = "Ch$ch", selected = filterChannel == ch,
                        onClick = { filterChannel = if (filterChannel == ch) 0 else ch })
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MidiMessageType.entries.forEach { type ->
                    FilterChip(
                        label = type.label,
                        selected = filterType == type,
                        onClick = { filterType = if (filterType == type) null else type }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MidiColors.CardBorder)
            Spacer(Modifier.height(4.dp))

            // Log list
            LazyColumn(
                modifier = Modifier.height(180.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filtered) { entry ->
                    MonitorRow(entry)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MidiColors.PurpleGlow else MidiColors.SurfaceCard
    val textColor = if (selected) Color.White else MidiColors.TextSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = textColor, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
    }
}

@Composable
private fun MonitorRow(entry: MidiLogEntry) {
    val dirColor = if (entry.direction == LogDirection.OUT) MidiColors.CyanAccent else MidiColors.GreenActive
    val typeColor = when (entry.messageType) {
        MidiMessageType.NOTE_ON        -> MidiColors.GreenActive
        MidiMessageType.NOTE_OFF       -> MidiColors.TextMuted
        MidiMessageType.CONTROL_CHANGE -> MidiColors.CyanAccent
        MidiMessageType.PROGRAM_CHANGE -> MidiColors.PurpleLight
        MidiMessageType.SYSEX          -> MidiColors.OrangeWarm
    }
    val time = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(if (entry.direction == LogDirection.OUT) "▶" else "◀",
            color = dirColor, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            modifier = Modifier.width(10.dp))
        Text(time, color = MidiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace))
        Text(entry.messageType.label, color = typeColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(30.dp))
        Text("Ch${entry.channel}", color = MidiColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            modifier = Modifier.width(24.dp))
        Text(entry.rawBytes, color = MidiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace),
            maxLines = 1)
    }
}
