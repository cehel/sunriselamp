package com.zuehlke.sunriselamp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform