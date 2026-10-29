package com.midicommander.app.ui.screens

import android.media.midi.MidiDeviceInfo
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.midicommander.app.data.model.*
import com.midicommander.app.midi.MidiConnectionState
import com.midicommander.app.ui.components.*
import com.midicommander.app.ui.theme.MidiColors
import com.midicommander.app.viewmodel.MainViewModel

// ─── Main Controller Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val midiLogEntries = remember { mutableStateListOf<MidiLogEntry>() }
    val availableDevices by viewModel.availableDevices.collectAsState()
    var showDevicePicker by remember { mutableStateOf(false) }

    // Collect MIDI log
    LaunchedEffect(Unit) {
        viewModel.midiLog.collect { entry ->
            midiLogEntries.add(0, entry)
            if (midiLogEntries.size > 200) midiLogEntries.removeAt(midiLogEntries.lastIndex)
        }
    }

    var configTarget by remember { mutableStateOf<ControlConfig?>(null) }
    val sliderValues = remember { mutableStateMapOf<String, Int>() }

    // Ensure controls exist for current bank grid
    val bank = uiState.banks.firstOrNull { it.id == uiState.selectedBankId }
    val rows = bank?.gridRows ?: 4
    val cols = bank?.gridCols ?: 4

    // Synthesize full grid - fill missing cells with defaults
    val gridControls: List<ControlConfig> = remember(uiState.controls, uiState.selectedBankId, rows, cols) {
        val existingMap = uiState.controls.associateBy { "${it.row}_${it.col}" }
        buildList {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    add(existingMap["${r}_${c}"] ?: ControlConfig(
                        id = "${uiState.selectedBankId}_${r}_${c}",
                        bankId = uiState.selectedBankId,
                        row = r, col = c,
                        label = "—"
                    ))
                }
            }
        }
    }

    // Separate sliders and knobs from buttons for the sidebar
    val buttons  = gridControls.filter { it.controlType == ControlType.BUTTON }
    val sliders  = gridControls.filter { it.controlType == ControlType.SLIDER }
    val knobs    = gridControls.filter { it.controlType == ControlType.KNOB }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(MidiColors.DeepBlack, MidiColors.SurfaceDark))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────
            AppHeader(
                connectionState = connectionState,
                editMode = uiState.editMode,
                showMonitor = uiState.showMonitor,
                onToggleEdit = viewModel::toggleEditMode,
                onToggleMonitor = viewModel::toggleMonitor,
                onDevicePickerClick = { showDevicePicker = true },
                onSettingsClick = onNavigateSettings
            )

            // ── Bank Selector ────────────────────────────────────────────────
            BankSelector(
                banks = uiState.banks,
                selectedBankId = uiState.selectedBankId,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                onSelectBank = { viewModel.selectBank(it.id) }
            )

            HorizontalDivider(color = MidiColors.CardBorder, modifier = Modifier.padding(horizontal = 12.dp))

            // ── Main content area ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Button Grid ──────────────────────────────────────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gridControls.filter { it.controlType == ControlType.BUTTON }) { control ->
                        MidiButton(
                            control = control,
                            editMode = uiState.editMode,
                            onPress = { viewModel.sendControl(it) },
                            onLongPress = { configTarget = it }
                        )
                    }
                }

                // ── Drawbars Sidebar ─────────────────────────────────────────
                if (sliders.isNotEmpty() || knobs.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidiColors.SurfaceCard)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (sliders.isNotEmpty()) {
                            Text("DRAWBARS", color = MidiColors.TextMuted,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sliders.forEach { control ->
                                    MidiDrawbar(
                                        control = control,
                                        currentValue = sliderValues[control.id] ?: control.secondaryValue,
                                        editMode = uiState.editMode,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        onValueChange = { v ->
                                            sliderValues[control.id] = v
                                            viewModel.sendSliderCc(control, v)
                                        },
                                        onLongPress = { configTarget = it }
                                    )
                                }
                            }
                        }

                        if (knobs.isNotEmpty()) {
                            Text("KNOBS", color = MidiColors.TextMuted,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                knobs.forEach { control ->
                                    MidiKnob(
                                        control = control,
                                        currentValue = sliderValues[control.id] ?: control.secondaryValue,
                                        editMode = uiState.editMode,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = { v ->
                                            sliderValues[control.id] = v
                                            viewModel.sendSliderCc(control, v)
                                        },
                                        onLongPress = { configTarget = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── MIDI Monitor (collapsible) ───────────────────────────────────
            AnimatedVisibility(
                visible = uiState.showMonitor,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(250)),
                exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
            ) {
                MidiMonitorPanel(
                    entries = midiLogEntries,
                    onClose = viewModel::toggleMonitor
                )
            }
        }

        // ── Edit mode hint bar ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.editMode,
            enter = fadeIn(),
            exit  = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = MidiColors.AmberWarm.copy(0.15f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                border = BorderStroke(1.dp, MidiColors.AmberWarm.copy(0.4f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null,
                        tint = MidiColors.AmberWarm, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Long-press any control to configure it",
                        color = MidiColors.AmberWarm,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    // ── Device Picker Dialog ─────────────────────────────────────────────────
    if (showDevicePicker) {
        DevicePickerDialog(
            devices = availableDevices,
            onDismiss = { showDevicePicker = false },
            onSelect = { info, port ->
                viewModel.connectToDevice(info, port)
                showDevicePicker = false
            }
        )
    }

    // ── Control Config Dialog ────────────────────────────────────────────────
    configTarget?.let { target ->
        ControlConfigDialog(
            control = target,
            onDismiss = { configTarget = null },
            onSave = { updated ->
                viewModel.upsertControl(updated)
                configTarget = null
            },
            onReset = { viewModel.resetControl(it) },
            onStartMidiLearn = viewModel::startMidiLearn
        )
    }
}

// ─── App Header ───────────────────────────────────────────────────────────────

@Composable
fun AppHeader(
    connectionState: MidiConnectionState,
    editMode: Boolean,
    showMonitor: Boolean,
    onToggleEdit: () -> Unit,
    onToggleMonitor: () -> Unit,
    onDevicePickerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val (statusText, statusColor) = when (connectionState) {
        is MidiConnectionState.Connected ->
            "● ${connectionState.deviceName}  [${connectionState.portCount}p]" to MidiColors.GreenActive
        is MidiConnectionState.Connecting ->
            "◌ Connecting…" to MidiColors.AmberWarm
        is MidiConnectionState.Error ->
            "✕ ${connectionState.message}" to MidiColors.RedAlert
        MidiConnectionState.Disconnected ->
            "○ No Device Connected" to MidiColors.TextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(MidiColors.SurfaceDark, MidiColors.SurfaceMid)
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // App title
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                "MIDI Commander Pro",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MidiColors.TextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            )
            Text(
                statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = statusColor,
                    fontSize = 10.sp
                )
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // USB device picker
            IconButton(onClick = onDevicePickerClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Usb, contentDescription = "Pick Device",
                    tint = when (connectionState) {
                        is MidiConnectionState.Connected -> MidiColors.GreenActive
                        else -> MidiColors.TextSecondary
                    },
                    modifier = Modifier.size(18.dp))
            }

            // Monitor toggle
            IconToggleButton(
                checked = showMonitor,
                onCheckedChange = { onToggleMonitor() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = "Monitor",
                    tint = if (showMonitor) MidiColors.CyanAccent else MidiColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Edit mode toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (editMode) MidiColors.AmberWarm.copy(0.2f) else MidiColors.SurfaceCard)
                    .border(
                        1.dp,
                        if (editMode) MidiColors.AmberWarm else MidiColors.CardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onToggleEdit)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit",
                        tint = if (editMode) MidiColors.AmberWarm else MidiColors.TextSecondary,
                        modifier = Modifier.size(13.dp))
                    Text(
                        if (editMode) "EDIT ON" else "EDIT",
                        color = if (editMode) MidiColors.AmberWarm else MidiColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Settings
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings",
                    tint = MidiColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
