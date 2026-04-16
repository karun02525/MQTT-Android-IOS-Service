package org.smarthome.models
import androidx.compose.ui.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// ─── OBD Command → publishes to vehicle/commands ──────────────────
//
//  JSON sent:
//  { "command": "010C", "deviceId": "android_abc123" }
//
data class OBDPayload(
    val command : String,   // "010C" = RPM  |  "010D" = Speed
    val deviceId: String
) {
    // Build JSON string manually — no extra lib needed
    fun toJson(): String = """{"command":"$command","deviceId":"$deviceId"}"""
}

// ─── GPS Payload → publishes to vehicle/gps ───────────────────────
//
//  JSON sent:
//  { "lat": 19.0760, "lng": 72.8777, "deviceId": "android_abc123" }
//
data class GPSPayload(
    val lat     : Double,
    val lng     : Double,
    val deviceId: String
) {
    fun toJson(): String = """{"lat":$lat,"lng":$lng,"deviceId":"$deviceId"}"""
}

// ─── Response received ← from vehicle/response ────────────────────
//
//  JSON received:
//  { "speed":62, "rpm":2100, "lat":19.07, "lng":72.87,
//    "timestamp":"2026-04-15T17:12:00", "deviceId":"android_abc123" }
//
data class VehicleResponse(
    val speed    : Int    = 0,
    val rpm      : Int    = 0,
    val lat      : Double = 0.0,
    val lng      : Double = 0.0,
    val timestamp: String = "",
    val deviceId : String = ""
)

// ─── Log entry for sent messages (Log tab) ────────────────────────
data class SentMessage(
    val id        : String = UUID.randomUUID().toString(),
    val topic     : String,
    val payload   : String,
    val timeLabel : String = LocalTime.now()
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
)

// ─── Connection state ─────────────────────────────────────────────
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED;

    val label: String
        get() = when (this) {
            DISCONNECTED -> "Disconnected"
            CONNECTING   -> "Connecting..."
            CONNECTED    -> "Connected"
            FAILED       -> "Connection Failed"
        }

    val isConnected: Boolean
        get() = this == CONNECTED

    // Color for UI — used in ConnectionBanner
    val color: Color
        get() = when (this) {
            CONNECTED    -> Color(0xFF4CAF50)   // Green
            CONNECTING   -> Color(0xFFFF9800)   // Orange
            DISCONNECTED -> Color(0xFF9E9E9E)   // Gray
            FAILED       -> Color(0xFFE53935)   // Red
        }
}