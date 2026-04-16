package org.smarthome// src/main/kotlin/VehicleBridge.kt

import kotlinx.serialization.json.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.time.LocalDateTime

class VehicleBridge(
    private val brokerUrl: String = "tcp://localhost:1883"
) {
    private val client = MqttClient(brokerUrl, "ktor-bridge", MemoryPersistence())

    // ── In-memory cache per device ───────────────────────────
    // obdCache["device_001"] = { "010C" -> 2100, "010D" -> 62 }
    private val obdCache = mutableMapOf<String, MutableMap<String, Int>>()
    // gpsCache["device_001"] = Pair(19.0760, 72.8777)
    private val gpsCache = mutableMapOf<String, Pair<Double, Double>>()

    companion object {
        const val TOPIC_COMMANDS = "vehicle/commands"
        const val TOPIC_GPS      = "vehicle/gps"
        const val TOPIC_RESPONSE = "vehicle/response"
    }

    // ── Connect and process incoming messages ─────────────────
    fun connectAndProcess() {
        val options = MqttConnectOptions().apply {
            isCleanSession       = true
            keepAliveInterval    = 60
            isAutomaticReconnect = true
        }

        client.setCallback(object : MqttCallback {

            override fun connectionLost(cause: Throwable?) {
                println("⚠️ [KTOR BRIDGE] Connection lost: ${cause?.message}")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload)
                println("\n📨 [KTOR BRIDGE] Received [$topic]")
                println("   Payload: $payload")

                // Route to correct handler
                when (topic) {
                    TOPIC_COMMANDS -> processOBDCommand(payload)
                    TOPIC_GPS      -> processGPS(payload)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        client.connect(options)

        // Subscribe to both Android topics
        client.subscribe(TOPIC_COMMANDS, 1)
        client.subscribe(TOPIC_GPS, 1)

        println("✅ [KTOR BRIDGE] Connected → processing: $TOPIC_COMMANDS + $TOPIC_GPS")
    }

    // ── Handle OBD Command (010C / 010D) ─────────────────────
    private fun processOBDCommand(payload: String) {
        val json     = Json.parseToJsonElement(payload).jsonObject
        val command  = json["command"] ?.jsonPrimitive?.content?.uppercase() ?: return
        val deviceId = json["deviceId"]?.jsonPrimitive?.content ?: "unknown"

        // Simulate OBD ECU response
        // In real app: parse actual hex bytes from OBD2 adapter
        val simulatedValue = when (command) {
            "010C" -> (800..4000).random()   // RPM — OBD formula: ((A*256)+B)/4
            "010D" -> (20..120).random()     // Speed (km/h) — OBD formula: A
            else   -> {
                println("⚠️ [KTOR BRIDGE] Unknown PID: $command")
                return
            }
        }

        // Store in cache
        obdCache.getOrPut(deviceId) { mutableMapOf() }[command] = simulatedValue

        val label = if (command == "010C") "RPM" else "Speed"
        println("   🔧 [KTOR BRIDGE] PID $command ($label) → $simulatedValue")

        // Try to build + publish full response
        tryPublishResponse(deviceId)
    }

    // ── Handle GPS Data ───────────────────────────────────────
    private fun processGPS(payload: String) {
        val json     = Json.parseToJsonElement(payload).jsonObject
        val lat      = json["lat"]     ?.jsonPrimitive?.double ?: 0.0
        val lng      = json["lng"]     ?.jsonPrimitive?.double ?: 0.0
        val deviceId = json["deviceId"]?.jsonPrimitive?.content ?: "unknown"

        // Store in cache
        gpsCache[deviceId] = Pair(lat, lng)
        println("   📍 [KTOR BRIDGE] GPS cached: lat=$lat, lng=$lng")

        tryPublishResponse(deviceId)
    }

    // ── Build + Publish full response to iOS ──────────────────
    // Publishes when we have at least RPM + Speed (GPS is optional, defaults to 0.0)
    private fun tryPublishResponse(deviceId: String) {
        val obd   = obdCache[deviceId] ?: return   // No OBD data yet
        val rpm   = obd["010C"]        ?: return   // No RPM yet
        val speed = obd["010D"]        ?: return   // No Speed yet
        val gps   = gpsCache[deviceId] ?: Pair(0.0, 0.0)   // GPS is optional

        // Build the JSON response
        val responseJson = buildJsonObject {
            put("speed",     speed)
            put("rpm",       rpm)
            put("lat",       gps.first)
            put("lng",       gps.second)
            put("timestamp", LocalDateTime.now().toString())
            put("deviceId",  deviceId)
        }.toString()

        // Publish to vehicle/response → iOS receives this
        val msg = MqttMessage(responseJson.toByteArray()).apply {
            qos = 1
        }
        client.publish(TOPIC_RESPONSE, msg)

        // ✅ Clear cache after publish so next iOS update
        //    requires a fresh full set from Android (RPM + Speed + GPS)
        obdCache.remove(deviceId)
        gpsCache.remove(deviceId)

        println("   📤 [KTOR BRIDGE] Published → $TOPIC_RESPONSE")
        println("   JSON: $responseJson")
        println("   🔄 Cache cleared for deviceId=$deviceId")
    }

    fun disconnect() {
        if (client.isConnected) client.disconnect()
        println("🛑 [KTOR BRIDGE] Disconnected")
    }
}