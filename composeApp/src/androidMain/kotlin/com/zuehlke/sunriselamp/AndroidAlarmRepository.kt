package com.zuehlke.sunriselamp

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import com.zuehlke.sunriselamp.model.AlarmInfo
import com.zuehlke.sunriselamp.model.AlarmSource
import com.zuehlke.sunriselamp.model.Day
import java.util.Calendar

/**
 * Reads alarms from the system Clock app.
 *
 * On Android 12+ (including Pixel with Google Clock) all Clock content providers
 * are protected by a signature-level permission and are inaccessible to third-party
 * apps. The code below still tries the known URIs so it works on AOSP / custom ROMs
 * and falls back to [AlarmManager.getNextAlarmClock] on modern Pixel / Samsung devices.
 *
 * AOSP / Google DeskClock DAYS_OF_WEEK bitmask (bit 0 = Monday … bit 6 = Sunday):
 *   Mon=0x01, Tue=0x02, Wed=0x04, Thu=0x08, Fri=0x10, Sat=0x20, Sun=0x40
 */
@SuppressLint("MissingPermission")
class AndroidAlarmRepository(private val context: Context) : AlarmRepository {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun getAlarms(): List<AlarmInfo> =
        queryProviders().ifEmpty { nextAlarmFallback() }

    // ── Content providers (AOSP / custom ROMs only) ───────────────────────────

    private val knownAuthorities = listOf(
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage"
    )

    private fun queryProviders(): List<AlarmInfo> {
        for (authority in knownAuthorities) {
            val result = queryAuthority(authority)
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    private fun queryAuthority(authority: String): List<AlarmInfo> {
        val uri        = Uri.parse("content://$authority/alarms")
        val projection = arrayOf("_id", "hour", "minutes", "daysofweek", "enabled", "label")
        return try {
            context.contentResolver.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                AlarmInfo(
                                    id      = cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                                    hour    = cursor.getInt(cursor.getColumnIndexOrThrow("hour")),
                                    minute  = cursor.getInt(cursor.getColumnIndexOrThrow("minutes")),
                                    days    = decodeDaysOfWeek(cursor.getInt(cursor.getColumnIndexOrThrow("daysofweek"))),
                                    enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1,
                                    label   = cursor.getString(cursor.getColumnIndexOrThrow("label")) ?: "",
                                    source  = AlarmSource.CLOCK_APP
                                )
                            )
                        }
                    }
                } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── AlarmManager fallback ─────────────────────────────────────────────────

    /**
     * Returns the single next-firing alarm from [AlarmManager].
     * Available on all devices but only gives one alarm with no repeat-day information.
     */
    private fun nextAlarmFallback(): List<AlarmInfo> {
        val info = alarmManager.nextAlarmClock ?: return emptyList()
        val cal  = Calendar.getInstance().apply { timeInMillis = info.triggerTime }
        return listOf(
            AlarmInfo(
                id      = info.triggerTime,
                hour    = cal.get(Calendar.HOUR_OF_DAY),
                minute  = cal.get(Calendar.MINUTE),
                days    = emptySet(),
                enabled = true,
                label   = "",
                source  = AlarmSource.ALARM_MANAGER
            )
        )
    }

    // ── Bitmask decoder ───────────────────────────────────────────────────────

    private fun decodeDaysOfWeek(bitmask: Int): Set<Day> = buildSet {
        if (bitmask and 0x01 != 0) add(Day.MON)
        if (bitmask and 0x02 != 0) add(Day.TUE)
        if (bitmask and 0x04 != 0) add(Day.WED)
        if (bitmask and 0x08 != 0) add(Day.THU)
        if (bitmask and 0x10 != 0) add(Day.FRI)
        if (bitmask and 0x20 != 0) add(Day.SAT)
        if (bitmask and 0x40 != 0) add(Day.SUN)
    }
}
