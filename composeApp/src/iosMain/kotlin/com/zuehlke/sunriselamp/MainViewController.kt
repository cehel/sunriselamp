package com.zuehlke.sunriselamp

import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zuehlke.sunriselamp.ble.IosBleManager

fun MainViewController() = ComposeUIViewController {
    val vm: BleViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                BleViewModel(
                    bleManager      = IosBleManager(),
                    alarmRepository = IosAlarmRepository()
                )
            }
        }
    )
    // iOS handles BLE permissions via Info.plist — no runtime launcher needed
    BleScreen(vm = vm, onScanRequested = { vm.scan() })
}