package com.zuehlke.sunriselamp

import com.zuehlke.sunriselamp.model.AlarmInfo

/** Platform-specific provider for alarms stored in the system clock app. */
interface AlarmRepository {
    fun getAlarms(): List<AlarmInfo>
}
