package org.smarthome// src/main/kotlin/VehicleSubscriber.kt

import kotlinx.serialization.json.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class VehicleSubscriber(
    private val brokerUrl: String = "tcp://localhost:1883"
) {
    private val client = MqttClient(brokerUrl, "ios-subscriber", MemoryPersistence())

    companion object {
        const val TOPIC_RESPONSE = "vehicle/response"
    }

    // ── Connect and Subscribe ────────────────────────────────
    fun connectAndListen() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
        }

        // Set message callback BEFORE connecting
        client.setCallback(object : MqttCallback {

            override fun connectionLost(cause: Throwable?) {
                println("⚠️ [iOS] Connection lost: ${cause?.message}")
            }

            // Called every time a message arrives
            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload)
                when (topic) {
                    TOPIC_RESPONSE -> displayVehicleData(payload)
                    else           -> println("📨 [iOS] [$topic]: $payload")
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        client.connect(options)

        // Subscribe to vehicle/response
        client.subscribe(TOPIC_RESPONSE, 1)

        println("✅ [iOS] Subscriber connected → listening on: $TOPIC_RESPONSE")
    }

    // ── Display received vehicle data ────────────────────────
    private fun displayVehicleData(payload: String) {
        try {
            val json = Json.parseToJsonElement(payload).jsonObject

            val speed     = json["speed"]    ?.jsonPrimitive?.int     ?: 0
            val rpm       = json["rpm"]      ?.jsonPrimitive?.int     ?: 0
            val lat       = json["lat"]      ?.jsonPrimitive?.double  ?: 0.0
            val lng       = json["lng"]      ?.jsonPrimitive?.double  ?: 0.0
            val timestamp = json["timestamp"]?.jsonPrimitive?.content ?: ""
            val deviceId  = json["deviceId"] ?.jsonPrimitive?.content ?: ""

            println("""
                ╔══════════════════════════════════════════╗
                ║         📱 iOS — DATA RECEIVED!          ║
                ╠══════════════════════════════════════════╣
                ║  🏎  Speed    : $speed km/h
                ║  ⚙️  RPM      : $rpm
                ║  📍 Latitude  : $lat
                ║  📍 Longitude : $lng
                ║  🕒 Timestamp : $timestamp
                ║  📱 Device ID : $deviceId
                ╚══════════════════════════════════════════╝
            """.trimIndent())

        } catch (e: Exception) {
            println("❌ [iOS] JSON parse error: ${e.message}")
            println("   Raw payload: $payload")
        }
    }

    fun disconnect() {
        if (client.isConnected) client.disconnect()
        println("🛑 [iOS] Subscriber disconnected")
    }
}