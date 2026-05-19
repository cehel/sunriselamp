package com.zuehlke.sunriselamp

import com.zuehlke.sunriselamp.model.AlarmInfo

/** iOS stub — alarm access via EventKit is a future enhancement. */
class IosAlarmRepository : AlarmRepository {
    override fun getAlarms(): List<AlarmInfo> = emptyList()
}
