package com.midicommander.app.midi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val ACTION_USB_PERMISSION = "com.midicommander.app.USB_PERMISSION"

@Singleton
class UsbMidiDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val midiEngine: MidiEngine
) {
    companion object {
        private const val TAG = "UsbMidiDeviceMgr"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager

    private val _availableDevices = MutableStateFlow<List<MidiDeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<MidiDeviceInfo>> = _availableDevices.asStateFlow()

    // ─── USB Permission Receiver ──────────────────────────────────────────────

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted && device != null) {
                    openMidiDevice(device)
                } else {
                    Log.w(TAG, "USB permission denied for ${device?.deviceName}")
                }
            }
        }
    }

    // ─── MIDI Device Callback ─────────────────────────────────────────────────

    private val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo) {
                Log.d(TAG, "MIDI device added: ${device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
                refreshDeviceList()
                // Auto-connect to first device with input ports
                if (device.inputPortCount > 0) {
                    requestPermissionAndConnect(device)
                }
            }

            override fun onDeviceRemoved(device: MidiDeviceInfo) {
                Log.d(TAG, "MIDI device removed")
                refreshDeviceList()
                midiEngine.disconnect()
            }
        }
    } else null

    // ─── Init ─────────────────────────────────────────────────────────────────

    fun initialize() {
        if (!midiEngine.hasUsbHostSupport()) {
            Log.w(TAG, "No USB Host support")
            return
        }

        // Register USB permission receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbPermissionReceiver, filter)
        }

        // Register MIDI device callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceCallback?.let { midiManager?.registerDeviceCallback(it, null) }
        }

        // Scan already connected devices
        refreshDeviceList()
        autoConnectExistingDevice()
    }

    fun shutdown() {
        try {
            context.unregisterReceiver(usbPermissionReceiver)
        } catch (_: Exception) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceCallback?.let { midiManager?.unregisterDeviceCallback(it) }
        }
        midiEngine.shutdown()
    }

    // ─── Device Discovery ─────────────────────────────────────────────────────

    private fun refreshDeviceList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = midiManager?.devices?.toList() ?: emptyList()
            _availableDevices.value = devices
        }
    }

    private fun autoConnectExistingDevice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val devices = midiManager?.devices ?: return
        val candidate = devices.firstOrNull { it.inputPortCount > 0 } ?: return
        requestPermissionAndConnect(candidate)
    }

    private fun requestPermissionAndConnect(info: MidiDeviceInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        midiManager?.openDevice(info, { device ->
            if (device != null) {
                midiEngine.connect(device)
            }
        }, null)
    }

    // ─── USB Device from Intent (ACTION_USB_DEVICE_ATTACHED) ─────────────────

    fun handleUsbDeviceAttached(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            openMidiDevice(device)
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val permIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
            usbManager.requestPermission(device, permIntent)
        }
    }

    private fun openMidiDevice(usbDevice: UsbDevice) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val deviceInfo = midiManager?.devices?.firstOrNull { info ->
            // Match by the UsbDevice reference stored in the MIDI device properties
            val midiUsbDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info.properties.getParcelable(MidiDeviceInfo.PROPERTY_USB_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                info.properties.getParcelable(MidiDeviceInfo.PROPERTY_USB_DEVICE) as? UsbDevice
            }
            midiUsbDevice?.deviceId == usbDevice.deviceId
        } ?: run {
            Log.w(TAG, "Could not match USB device to MIDI device info")
            return
        }
        midiManager?.openDevice(deviceInfo, { device ->
            if (device != null) midiEngine.connect(device)
        }, null)
    }

    fun connectToDevice(info: MidiDeviceInfo, portIndex: Int = 0) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        midiManager?.openDevice(info, { device ->
            if (device != null) midiEngine.connect(device, portIndex)
        }, null)
    }
}
