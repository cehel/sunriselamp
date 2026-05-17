package com.zuehlke.sunriselamp.ble

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreBluetooth.*
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosBleManager : BleManager {

    private val _state = MutableStateFlow<BleState>(BleState.Idle)
    override val state: StateFlow<BleState> = _state

    private val _log = MutableStateFlow<List<String>>(emptyList())
    override val log: StateFlow<List<String>> = _log

    private var centralManager: CBCentralManager? = null
    private var peripheral: CBPeripheral? = null
    private var writeCharacteristic: CBCharacteristic? = null

    // ── Central delegate ──────────────────────────────────────────────────────

    private val centralDelegate = object : NSObject(), CBCentralManagerDelegateProtocol {

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state == CBManagerStatePoweredOn && _state.value is BleState.Scanning) {
                doScan(central)
            } else if (central.state != CBManagerStatePoweredOn && _state.value is BleState.Scanning) {
                _state.value = BleState.Error("Bluetooth unavailable (state=${central.state})")
            }
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber
        ) {
            central.stopScan()
            appendLog("Found ${didDiscoverPeripheral.name ?: "device"} — connecting…")
            peripheral = didDiscoverPeripheral
            _state.value = BleState.Connecting
            central.connectPeripheral(didDiscoverPeripheral, options = null)
        }

        override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
            appendLog("Connected — discovering services…")
            didConnectPeripheral.delegate = peripheralDelegate
            didConnectPeripheral.discoverServices(
                listOf(CBUUID.UUIDWithString(BleConstants.SERVICE_UUID))
            )
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            writeCharacteristic = null
            _state.value = BleState.Disconnected
            appendLog("Disconnected")
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            _state.value = BleState.Error(
                "Failed to connect: ${error?.localizedDescription ?: "unknown"}"
            )
        }
    }

    // ── Peripheral delegate ───────────────────────────────────────────────────

    private val peripheralDelegate = object : NSObject(), CBPeripheralDelegateProtocol {

        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            if (didDiscoverServices != null) {
                _state.value = BleState.Error(
                    "Service discovery failed: ${didDiscoverServices.localizedDescription}"
                )
                return
            }

            val service = peripheral.services
                ?.filterIsInstance<CBService>()
                ?.find { it.UUID.UUIDString.equals(BleConstants.SERVICE_UUID, ignoreCase = true) }

            if (service == null) {
                _state.value = BleState.Error("Echo service not found on device")
                return
            }

            peripheral.discoverCharacteristics(
                listOf(CBUUID.UUIDWithString(BleConstants.CHAR_UUID)),
                forService = service
            )
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: NSError?
        ) {
            if (error != null) {
                _state.value = BleState.Error(
                    "Characteristic discovery failed: ${error.localizedDescription}"
                )
                return
            }

            val char = didDiscoverCharacteristicsForService.characteristics
                ?.filterIsInstance<CBCharacteristic>()
                ?.find { it.UUID.UUIDString.equals(BleConstants.CHAR_UUID, ignoreCase = true) }

            if (char == null) {
                _state.value = BleState.Error("Echo characteristic not found")
                return
            }

            writeCharacteristic = char
            _state.value = BleState.Connected
            appendLog("Ready to send messages")
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didWriteValueForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
            if (error == null) {
                appendLog("✓ Write confirmed by Pi")
            } else {
                appendLog("✗ Write failed: ${error.localizedDescription}")
            }
        }
    }

    // ── BleManager impl ───────────────────────────────────────────────────────

    override fun startScan() {
        _state.value = BleState.Scanning
        appendLog("Scanning for ${BleConstants.DEVICE_NAME}…")

        if (centralManager == null) {
            // Scanning is deferred to centralManagerDidUpdateState when state == PoweredOn
            centralManager = CBCentralManager(delegate = centralDelegate, queue = null)
        } else if (centralManager!!.state == CBManagerStatePoweredOn) {
            doScan(centralManager!!)
        }
    }

    override fun disconnect() {
        val p = peripheral ?: return
        centralManager?.cancelPeripheralConnection(p)
    }

    override fun sendMessage(text: String): Boolean {
        val char = writeCharacteristic ?: return false
        val p    = peripheral          ?: return false

        appendLog("→ Sending: \"$text\"")

        @Suppress("CAST_NEVER_SUCCEEDS")
        val data = (text as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return false
        p.writeValue(data, forCharacteristic = char, type = CBCharacteristicWriteWithResponse)
        return true
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun doScan(central: CBCentralManager) {
        central.scanForPeripheralsWithServices(
            serviceUUIDs = listOf(CBUUID.UUIDWithString(BleConstants.SERVICE_UUID)),
            options = null
        )
    }

    private fun appendLog(msg: String) {
        _log.value = (_log.value + msg).takeLast(50)
    }
}
