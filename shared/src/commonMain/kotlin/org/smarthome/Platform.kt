package org.smarthome

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform