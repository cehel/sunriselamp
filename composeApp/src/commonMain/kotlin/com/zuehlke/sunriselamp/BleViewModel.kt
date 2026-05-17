package com.zuehlke.sunriselamp

import androidx.lifecycle.ViewModel
import com.zuehlke.sunriselamp.ble.BleManager
import com.zuehlke.sunriselamp.ble.BleState
import kotlinx.coroutines.flow.StateFlow

class BleViewModel(private val bleManager: BleManager) : ViewModel() {

    val state: StateFlow<BleState>       = bleManager.state
    val log:   StateFlow<List<String>>   = bleManager.log

    fun scan()               = bleManager.startScan()
    fun disconnect()         = bleManager.disconnect()
    fun send(text: String)   = bleManager.sendMessage(text)

    override fun onCleared() {
        bleManager.disconnect()
        super.onCleared()
    }
}
