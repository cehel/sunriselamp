package com.zuehlke.sunriselamp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zuehlke.sunriselamp.ble.BleManager
import com.zuehlke.sunriselamp.ble.BleState
import kotlinx.coroutines.flow.StateFlow

class BleViewModel(app: Application) : AndroidViewModel(app) {

    private val bleManager = BleManager(app)

    val state: StateFlow<BleState> = bleManager.state
    val log:   StateFlow<List<String>> = bleManager.log

    fun scan()           = bleManager.startScan()
    fun disconnect()     = bleManager.disconnect()
    fun send(text: String) = bleManager.sendMessage(text)

    override fun onCleared() {
        bleManager.disconnect()
        super.onCleared()
    }
}
