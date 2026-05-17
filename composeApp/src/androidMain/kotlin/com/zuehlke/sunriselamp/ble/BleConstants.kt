package com.zuehlke.sunriselamp.ble

import java.util.UUID

object BleConstants {
    const val DEVICE_NAME = "RPI_ECHO"

    val SERVICE_UUID: UUID = UUID.fromString("A07498CA-AD5B-474E-940D-16F1FBE7E8CD")
    val CHAR_UUID: UUID    = UUID.fromString("51FF12BB-3ED8-46E5-B4F9-D64E2FEC021B")
}
