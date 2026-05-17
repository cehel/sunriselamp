package com.zuehlke.sunriselamp

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zuehlke.sunriselamp.ble.AndroidBleManager

private val blePermissions = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
)

/**
 * Android entry point for BleScreen. Handles runtime BLE permissions before
 * forwarding to the common [BleScreen] composable.
 */
@Composable
fun BleScreenWithPermissions() {
    val context = LocalContext.current
    val vm: BleViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BleViewModel(AndroidBleManager(context.applicationContext)) }
        }
    )

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) vm.scan()
    }

    BleScreen(
        vm             = vm,
        onScanRequested = { permLauncher.launch(blePermissions) }
    )
}
