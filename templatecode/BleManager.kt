package com.example.rpiecho.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ── Connection state ──────────────────────────────────────────────────────────

sealed class BleState {
    object Idle        : BleState()
    object Scanning    : BleState()
    object Connecting  : BleState()
    object Connected   : BleState()
    object Disconnected: BleState()
    data class Error(val message: String) : BleState()
}

// ── Manager ───────────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")   // Permissions are checked in the UI layer
class BleManager(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner get() = adapter.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    private val _state = MutableStateFlow<BleState>(BleState.Idle)
    val state: StateFlow<BleState> = _state

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    // ── Scan ─────────────────────────────────────────────────────────────────

    fun startScan() {
        _state.value = BleState.Scanning
        appendLog("Scanning for $DEVICE_NAME…")

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScan()
            appendLog("Found ${result.device.name ?: "device"} — connecting…")
            connect(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = BleState.Error("Scan failed (code $errorCode)")
        }
    }

    // ── Connect ───────────────────────────────────────────────────────────────

    private fun connect(device: BluetoothDevice) {
        _state.value = BleState.Connecting
        // autoConnect = false for faster initial connection
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    appendLog("Connected — discovering services…")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _state.value = BleState.Disconnected
                    characteristic = null
                    appendLog("Disconnected")
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = BleState.Error("Service discovery failed")
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                _state.value = BleState.Error("Echo service not found on device")
                return
            }

            characteristic = service.getCharacteristic(CHAR_UUID)
            if (characteristic == null) {
                _state.value = BleState.Error("Echo characteristic not found")
                return
            }

            _state.value = BleState.Connected
            appendLog("Ready to send messages")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendLog("✓ Write confirmed by Pi")
            } else {
                appendLog("✗ Write failed (status $status)")
            }
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    fun sendMessage(text: String): Boolean {
        val char = characteristic ?: return false
        val g    = gatt          ?: return false

        appendLog("→ Sending: \"$text\"")

        @Suppress("DEPRECATION")
        char.value     = text.toByteArray(Charsets.UTF_8)
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        @Suppress("DEPRECATION")
        return g.writeCharacteristic(char)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun appendLog(msg: String) {
        _log.value = (_log.value + msg).takeLast(50)   // keep last 50 lines
    }

    private companion object {
        val SERVICE_UUID = BleConstants.SERVICE_UUID
        val CHAR_UUID    = BleConstants.CHAR_UUID
        const val DEVICE_NAME = BleConstants.DEVICE_NAME
    }
}
