package com.midicommander.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.midicommander.app.data.model.GlobalSettings
import com.midicommander.app.data.model.Preset
import com.midicommander.app.ui.theme.MidiColors
import com.midicommander.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ─── Settings & Preset Screen ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var savePresetName by remember { mutableStateOf("") }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }

    // SysEx file picker
    val sysExLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            stream?.let { s -> viewModel.sendSysExFile(s) }
        }
    }

    Scaffold(
        containerColor = MidiColors.DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings & Presets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MidiColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MidiColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidiColors.SurfaceDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Global MIDI Settings ──────────────────────────────────────────
            item {
                SettingsSection("Global MIDI") {
                    // Channel Override
                    SettingsSlider(
                        label = "MIDI Channel Override",
                        value = settings.globalMidiChannel,
                        range = 0..16,
                        displayValue = if (settings.globalMidiChannel == 0) "Per-Control" else "CH${settings.globalMidiChannel}",
                        onChange = { viewModel.updateSettings(settings.copy(globalMidiChannel = it)) }
                    )
                    Spacer(Modifier.height(4.dp))
                    SettingsSlider(
                        label = "Default Cable / Port",
                        value = settings.defaultCableId,
                        range = 0..7,
                        displayValue = "Port ${settings.defaultCableId}",
                        onChange = { viewModel.updateSettings(settings.copy(defaultCableId = it)) }
                    )
                    Spacer(Modifier.height(4.dp))
                    SettingsToggle("MIDI Thru", settings.midiThru) {
                        viewModel.updateSettings(settings.copy(midiThru = it))
                    }
                    SettingsToggle("Native MIDI Driver (API 23+)", settings.useNativeMidi) {
                        viewModel.updateSettings(settings.copy(useNativeMidi = it))
                    }
                    SettingsSlider(
                        label = "SysEx Message Delay",
                        value = settings.sysExDelay.toInt(),
                        range = 0..500,
                        displayValue = "${settings.sysExDelay} ms",
                        onChange = { viewModel.updateSettings(settings.copy(sysExDelay = it.toLong())) }
                    )
                }
            }

            // ── App Settings ──────────────────────────────────────────────────
            item {
                SettingsSection("App") {
                    SettingsToggle("Haptic Feedback", settings.hapticFeedback) {
                        viewModel.updateSettings(settings.copy(hapticFeedback = it))
                    }
                    SettingsToggle("Dark Theme", settings.darkTheme) {
                        viewModel.updateSettings(settings.copy(darkTheme = it))
                    }
                    SettingsToggle("MIDI Learn Mode", settings.midiLearnEnabled) {
                        viewModel.updateSettings(settings.copy(midiLearnEnabled = it))
                    }
                }
            }

            // ── SysEx File ────────────────────────────────────────────────────
            item {
                SettingsSection("SysEx File") {
                    Button(
                        onClick = { sysExLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MidiColors.PurpleGlow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Load & Send .syx File")
                    }
                }
            }

            // ── Presets ───────────────────────────────────────────────────────
            item {
                SettingsSection("Presets") {
                    // Save
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = savePresetName,
                            onValueChange = { savePresetName = it },
                            placeholder = { Text("Preset name…", color = MidiColors.TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MidiColors.PurpleGlow,
                                unfocusedBorderColor = MidiColors.CardBorder,
                                focusedTextColor = MidiColors.TextPrimary,
                                unfocusedTextColor = MidiColors.TextPrimary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (savePresetName.isNotBlank()) {
                                    viewModel.savePreset(savePresetName)
                                    savePresetName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidiColors.GreenActive)
                        ) { Text("Save", color = Color.Black) }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Export / Import
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    importJson = viewModel.exportJson()
                                    showImportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MidiColors.CyanAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MidiColors.CyanAccent)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp)); Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MidiColors.OrangeWarm),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MidiColors.OrangeWarm)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp)); Text("Import JSON")
                        }
                    }
                }
            }

            // ── Preset List ───────────────────────────────────────────────────
            item {
                Text("Saved Presets", color = MidiColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            items(uiState.presets) { preset ->
                PresetRow(
                    preset = preset,
                    onLoad = { viewModel.loadPreset(preset) },
                    onDelete = { viewModel.deletePreset(preset) }
                )
            }
        }
    }

    // ── Import/Export Dialog ──────────────────────────────────────────────────
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = MidiColors.SurfaceDark,
            title = { Text("JSON Data", color = MidiColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = importJson,
                    onValueChange = { importJson = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MidiColors.PurpleGlow,
                        unfocusedBorderColor = MidiColors.CardBorder,
                        focusedTextColor = MidiColors.TextPrimary,
                        unfocusedTextColor = MidiColors.TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.importJson(importJson); showImportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MidiColors.PurpleGlow)
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = MidiColors.TextSecondary)
                }
            }
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MidiColors.SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MidiColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = MidiColors.PurpleLight,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
            HorizontalDivider(color = MidiColors.CardBorder)
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MidiColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MidiColors.PurpleGlow, checkedTrackColor = MidiColors.PurpleGlow.copy(0.4f))
        )
    }
}

@Composable
private fun SettingsSlider(label: String, value: Int, range: IntRange, displayValue: String, onChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MidiColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
            Text(displayValue, color = MidiColors.PurpleLight, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(thumbColor = MidiColors.PurpleGlow, activeTrackColor = MidiColors.PurpleGlow, inactiveTrackColor = MidiColors.CardBorder)
        )
    }
}

@Composable
private fun PresetRow(preset: Preset, onLoad: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MidiColors.SurfaceMid,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MidiColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = MidiColors.PurpleLight, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(preset.name, color = MidiColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onLoad) { Text("Load", color = MidiColors.GreenActive) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MidiColors.RedAlert, modifier = Modifier.size(16.dp))
            }
        }
    }
}
