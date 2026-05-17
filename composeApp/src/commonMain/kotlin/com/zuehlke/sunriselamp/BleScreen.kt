package com.zuehlke.sunriselamp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zuehlke.sunriselamp.ble.BleState
import com.zuehlke.sunriselamp.model.Day
import com.zuehlke.sunriselamp.model.WakeUpSchedule

@Composable
fun BleScreen(vm: BleViewModel, onScanRequested: () -> Unit) {
    val state by vm.state.collectAsState()
    val log   by vm.log.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.lastIndex)
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
                        else                  -> onScanRequested()
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
                AlarmConfigurator(onSend = { vm.sendAlarm(it) })
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
private fun AlarmConfigurator(onSend: (WakeUpSchedule) -> Unit) {
    var hour          by remember { mutableStateOf(7) }
    var minute        by remember { mutableStateOf(0) }
    var selectedDays  by remember { mutableStateOf(setOf<Day>()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text("Wake-up time", style = MaterialTheme.typography.titleMedium)

        // Time picker
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.fillMaxWidth()
        ) {
            TimeSpinner(
                value    = hour,
                range    = 0..23,
                onValue  = { hour = it },
                label    = "Hour"
            )
            Text(
                text     = ":",
                style    = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            TimeSpinner(
                value    = minute,
                range    = 0..59,
                onValue  = { minute = it },
                label    = "Min"
            )
        }

        // Weekday selector
        Text("Repeat on", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Day.entries.forEach { day ->
                val selected = day in selectedDays
                FilterChip(
                    selected = selected,
                    onClick  = {
                        selectedDays = if (selected) selectedDays - day else selectedDays + day
                    },
                    label    = { Text(day.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled  = selectedDays.isNotEmpty(),
            onClick  = { onSend(WakeUpSchedule(hour, minute, selectedDays)) }
        ) {
            Text("Set Alarm")
        }
    }
}

@Composable
private fun TimeSpinner(
    value:   Int,
    range:   IntRange,
    onValue: (Int) -> Unit,
    label:   String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        IconButton(onClick = { onValue(if (value < range.last) value + 1 else range.first) }) {
            Text("▲")
        }
        Text(
            text      = value.toString().padStart(2, '0'),
            style     = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier  = Modifier.widthIn(min = 64.dp)
        )
        IconButton(onClick = { onValue(if (value > range.first) value - 1 else range.last) }) {
            Text("▼")
        }
    }
}

@Composable
private fun StatusCard(state: BleState) {
    val (label, color) = when (state) {
        is BleState.Connected    -> "Connected"               to MaterialTheme.colorScheme.primary
        is BleState.Scanning     -> "Scanning"                to MaterialTheme.colorScheme.secondary
        is BleState.Connecting   -> "Connecting"              to MaterialTheme.colorScheme.secondary
        is BleState.Disconnected -> "Disconnected"            to MaterialTheme.colorScheme.error
        is BleState.Error        -> "Error: ${state.message}" to MaterialTheme.colorScheme.error
        is BleState.Idle         -> "Idle"                    to MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
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

