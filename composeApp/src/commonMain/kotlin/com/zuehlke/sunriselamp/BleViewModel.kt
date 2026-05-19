package com.zuehlke.sunriselamp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zuehlke.sunriselamp.ble.BleManager
import com.zuehlke.sunriselamp.ble.BleState
import com.zuehlke.sunriselamp.model.AlarmInfo
import com.zuehlke.sunriselamp.model.WakeUpSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BleViewModel(
    private val bleManager: BleManager,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    val state: StateFlow<BleState>     = bleManager.state
    val log:   StateFlow<List<String>> = bleManager.log

    private val _alarms = MutableStateFlow<List<AlarmInfo>>(emptyList())
    val alarms: StateFlow<List<AlarmInfo>> = _alarms

    private val _alarmsLoading = MutableStateFlow(false)
    val alarmsLoading: StateFlow<Boolean> = _alarmsLoading

    fun scan()                               = bleManager.startScan()
    fun disconnect()                         = bleManager.disconnect()
    fun sendAlarm(schedule: WakeUpSchedule)  = bleManager.sendMessage(schedule.toBleMessage())

    fun sendAlarmInfo(alarm: AlarmInfo)      = sendAlarm(alarm.toWakeUpSchedule())

    fun loadAlarms() {
        viewModelScope.launch {
            _alarmsLoading.value = true
            _alarms.value = withContext(Dispatchers.IO) { alarmRepository.getAlarms() }
            _alarmsLoading.value = false
        }
    }

    override fun onCleared() {
        bleManager.disconnect()
        super.onCleared()
    }
}
