package com.zuehlke.sunriselamp

import androidx.lifecycle.ViewModel
import com.zuehlke.sunriselamp.ble.BleManager
import com.zuehlke.sunriselamp.ble.BleState
import com.zuehlke.sunriselamp.model.WakeUpSchedule
import kotlinx.coroutines.flow.StateFlow

class BleViewModel(private val bleManager: BleManager) : ViewModel() {

    val state: StateFlow<BleState>       = bleManager.state
    val log:   StateFlow<List<String>>   = bleManager.log

    fun scan()                               = bleManager.startScan()
    fun disconnect()                         = bleManager.disconnect()
    fun sendAlarm(schedule: WakeUpSchedule)  = bleManager.sendMessage(schedule.toBleMessage())

    override fun onCleared() {
        bleManager.disconnect()
        super.onCleared()
    }
}
