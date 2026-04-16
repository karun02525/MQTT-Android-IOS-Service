package org.smarthome// src/main/kotlin/VehiclePublisher.kt

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class VehiclePublisher(
    private val brokerUrl: String = "tcp://localhost:1883",
    private val deviceId : String = "device_001"
) {
    // Each client MUST have a unique ID
    private val client = MqttClient(brokerUrl, "android-pub-$deviceId", MemoryPersistence())

    // ── Topics ───────────────────────────────────────────────
    companion object {
        const val TOPIC_COMMANDS = "vehicle/commands"
        const val TOPIC_GPS      = "vehicle/gps"
    }

    // ── Connect ──────────────────────────────────────────────
    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
        }
        client.connect(options)
        println("✅ [ANDROID] Publisher connected → $brokerUrl")
    }

    // ── Send RPM Command (OBD PID 010C) ──────────────────────
    // 010C = Engine RPM
    fun sendRPMCommand() {
        val payload = buildJsonObject {
            put("command",  "010C")
            put("deviceId", deviceId)
        }.toString()

        publish(TOPIC_COMMANDS, payload)
        println("📤 [ANDROID] OBD Command → 010C (Engine RPM Request)")
    }

    // ── Send Speed Command (OBD PID 010D) ────────────────────
    // 010D = Vehicle Speed
    fun sendSpeedCommand() {
        val payload = buildJsonObject {
            put("command",  "010D")
            put("deviceId", deviceId)
        }.toString()

        publish(TOPIC_COMMANDS, payload)
        println("📤 [ANDROID] OBD Command → 010D (Vehicle Speed Request)")
    }

    // ── Send GPS Location ─────────────────────────────────────
    fun sendGPS(lat: Double, lng: Double) {
        val payload = buildJsonObject {
            put("lat",      lat)
            put("lng",      lng)
            put("deviceId", deviceId)
        }.toString()

        publish(TOPIC_GPS, payload)
        println("📍 [ANDROID] GPS → lat=$lat, lng=$lng")
    }

    // ── Internal Publish Helper ───────────────────────────────
    private fun publish(topic: String, payload: String) {
        val msg = MqttMessage(payload.toByteArray()).apply {
            qos = 1  // At least once delivery
        }
        client.publish(topic, msg)
    }

    fun disconnect() {
        if (client.isConnected) client.disconnect()
        println("🛑 [ANDROID] Publisher disconnected")
    }
}