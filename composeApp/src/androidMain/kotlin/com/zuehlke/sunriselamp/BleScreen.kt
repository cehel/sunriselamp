package com.zuehlke.sunriselamp

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zuehlke.sunriselamp.ble.BleState
import kotlinx.coroutines.launch

private val blePermissions = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
)

@Composable
fun BleScreen(vm: BleViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val log   by vm.log.collectAsState()

    var inputText by remember { mutableStateOf("Hello, Raspberry Pi!") }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.lastIndex)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) vm.scan()
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text  = "Sunrise Lamp",
                style = MaterialTheme.typography.headlineMedium
            )

            StatusCard(state)

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick  = {
                    when (state) {
                        is BleState.Connected -> vm.disconnect()
                        else -> permLauncher.launch(blePermissions)
                    }
                }
            ) {
                Text(
                    when (state) {
                        is BleState.Connected  -> "Disconnect"
                        is BleState.Scanning   -> "Scanning…"
                        is BleState.Connecting -> "Connecting…"
                        else                   -> "Scan for Pi"
                    }
                )
            }

            AnimatedVisibility(visible = state is BleState.Connected) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        label         = { Text("Message") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            vm.send(inputText)
                            scope.launch { listState.animateScrollToItem(log.lastIndex) }
                        })
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick  = {
                            vm.send(inputText)
                            scope.launch { listState.animateScrollToItem(log.lastIndex) }
                        }
                    ) {
                        Text("Send")
                    }
                }
            }

            Text("Log", style = MaterialTheme.typography.labelLarge)

            LazyColumn(
                state    = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(log) { line ->
                    Text(
                        text       = line,
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: BleState) {
    val (label, color) = when (state) {
        is BleState.Connected    -> "Connected"    to MaterialTheme.colorScheme.primary
        is BleState.Scanning     -> "Scanning"     to MaterialTheme.colorScheme.secondary
        is BleState.Connecting   -> "Connecting"   to MaterialTheme.colorScheme.secondary
        is BleState.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.error
        is BleState.Error        -> "Error: ${state.message}" to MaterialTheme.colorScheme.error
        is BleState.Idle         -> "Idle"         to MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(label, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
