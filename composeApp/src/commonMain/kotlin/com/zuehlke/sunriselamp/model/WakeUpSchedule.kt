package com.zuehlke.sunriselamp.model

/**
 * Represents a wake-up alarm configuration to be sent to the Raspberry Pi.
 *
 * BLE message protocol (JSON):
 *   {"type":"alarm","hour":<0-23>,"minute":<0-59>,"days":["MON","TUE","WED","THU","FRI","SAT","SUN"]}
 *
 * Example:
 *   {"type":"alarm","hour":7,"minute":30,"days":["MON","WED","FRI"]}
 *
 * An empty "days" list means the alarm is disabled/cleared.
 */
data class WakeUpSchedule(
    val hour: Int,
    val minute: Int,
    val days: Set<Day>
) {
    init {
        require(hour   in 0..23) { "hour must be 0–23" }
        require(minute in 0..59) { "minute must be 0–59" }
    }

    fun toBleMessage(): String {
        val daysJson = days
            .sortedBy { it.ordinal }
            .joinToString(",") { "\"${it.name}\"" }
        return """{"type":"alarm","hour":$hour,"minute":$minute,"days":[$daysJson]}"""
    }
}

enum class Day(val label: String) {
    MON("Mo"),
    TUE("Tu"),
    WED("We"),
    THU("Th"),
    FRI("Fr"),
    SAT("Sa"),
    SUN("Su")
}
