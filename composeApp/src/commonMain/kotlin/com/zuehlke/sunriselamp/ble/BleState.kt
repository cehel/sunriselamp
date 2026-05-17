package com.zuehlke.sunriselamp.ble

sealed class BleState {
    object Idle         : BleState()
    object Scanning     : BleState()
    object Connecting   : BleState()
    object Connected    : BleState()
    object Disconnected : BleState()
    data class Error(val message: String) : BleState()
}
