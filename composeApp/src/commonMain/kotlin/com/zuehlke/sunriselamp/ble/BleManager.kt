package com.zuehlke.sunriselamp.ble

import kotlinx.coroutines.flow.StateFlow

interface BleManager {
    val state: StateFlow<BleState>
    val log: StateFlow<List<String>>
    fun startScan()
    fun disconnect()
    fun sendMessage(text: String): Boolean
}
