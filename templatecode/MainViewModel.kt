package com.example.rpiecho.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.rpiecho.ble.BleManager
import com.example.rpiecho.ble.BleState
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val bleManager = BleManager(app)

    val state: StateFlow<BleState> = bleManager.state
    val log:   StateFlow<List<String>> = bleManager.log

    fun scan()    = bleManager.startScan()
    fun disconnect() = bleManager.disconnect()

    fun send(text: String) {
        bleManager.sendMessage(text)
    }

    override fun onCleared() {
        bleManager.disconnect()
        super.onCleared()
    }
}
