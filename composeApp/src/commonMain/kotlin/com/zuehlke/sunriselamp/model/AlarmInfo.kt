package com.zuehlke.sunriselamp.model

/** Where the alarm data came from — drives the UI hint text. */
enum class AlarmSource { CLOCK_APP, ALARM_MANAGER }

/**
 * Alarm read from the device's system clock app via AlarmClockContract (Android)
 * or EventKit (iOS — future).
 */
data class AlarmInfo(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val days: Set<Day>,
    val label: String,
    val enabled: Boolean,
    val source: AlarmSource = AlarmSource.CLOCK_APP
) {
    fun toWakeUpSchedule() = WakeUpSchedule(hour, minute, days)

    fun formattedTime(): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
