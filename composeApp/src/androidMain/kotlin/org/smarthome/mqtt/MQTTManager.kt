package org.smarthome.mqtt

// mqtt/MQTTManager.kt

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

// ─── Callback interface → ViewModel listens to these ──────────────
interface MQTTListener {
    fun onConnected()
    fun onDisconnected(reason: String?)
    fun onMessageReceived(topic: String, payload: String)
    fun onPublishSuccess(topic: String, payload: String)
    fun onError(error: String)
}

class MQTTManager private constructor() {

    companion object {
        // ── Singleton ─────────────────────────────────────────────
        val shared = MQTTManager()

        // ── Topics ────────────────────────────────────────────────
        const val TOPIC_COMMANDS = "vehicle/commands"  // Android → Ktor
        const val TOPIC_GPS      = "vehicle/gps"       // Android → Ktor
        const val TOPIC_RESPONSE = "vehicle/response"  // Ktor → Android (subscribe)
    }

    var listener: MQTTListener? = null
    private var mqttClient: MqttClient? = null

    // ══════════════════════════════════════════════════════════════
    //  CONNECT
    //  ⚠️ BLOCKING — must be called from Dispatchers.IO
    //
    //  Android Emulator → host = "10.0.2.2"  (maps to 127.0.0.1 on PC/Mac)
    //  Real Device      → host = "192.168.x.x"  (your machine's WiFi IP)
    // ══════════════════════════════════════════════════════════════
    fun connect(host: String, port: Int = 1883, deviceId: String) {
        val brokerUrl = "tcp://$host:$port"
        val clientId  = "android-$deviceId"

        try {
            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

            // ── Set callbacks BEFORE connecting ───────────────────
            mqttClient?.setCallback(object : MqttCallback {

                // Called when connection drops unexpectedly
                override fun connectionLost(cause: Throwable?) {
                    println("⚠️ [MQTT] Connection lost: ${cause?.message}")
                    listener?.onDisconnected(cause?.message)
                }

                // Called when a subscribed message arrives
                override fun messageArrived(topic: String, message: MqttMessage) {
                    val payload = String(message.payload)
                    println("📥 [MQTT] [$topic] $payload")
                    listener?.onMessageReceived(topic, payload)
                }

                // Called when our published message is delivered
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    println("✅ [MQTT] Message delivered")
                }
            })

            // ── Connection options ─────────────────────────────────
            val options = MqttConnectOptions().apply {
                isCleanSession      = true
                connectionTimeout   = 10          // 10 seconds
                keepAliveInterval   = 30          // Ping every 30s
                isAutomaticReconnect = true       // Auto reconnect on drop
            }

            // ── Connect (BLOCKING) ────────────────────────────────
            mqttClient?.connect(options)

            // ── Subscribe to response topic ───────────────────────
            // So we can see what Ktor sends back (what iOS would receive)
            mqttClient?.subscribe(TOPIC_RESPONSE, 1)

            println("✅ [MQTT] Connected → $brokerUrl")
            println("📡 [MQTT] Subscribed → $TOPIC_RESPONSE")

            listener?.onConnected()

        } catch (e: MqttException) {
            println("❌ [MQTT] Connect error (${e.reasonCode}): ${e.message}")
            listener?.onError(e.message ?: "MQTT connection failed")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLISH
    // ══════════════════════════════════════════════════════════════
    fun publish(topic: String, payload: String, qos: Int = 1) {
        try {
            if (mqttClient?.isConnected != true) {
                listener?.onError("Not connected to broker")
                return
            }

            val msg = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
            }
            mqttClient?.publish(topic, msg)

            println("📤 [MQTT] [$topic] $payload")
            listener?.onPublishSuccess(topic, payload)

        } catch (e: MqttException) {
            println("❌ [MQTT] Publish error: ${e.message}")
            listener?.onError("Publish failed: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DISCONNECT
    // ══════════════════════════════════════════════════════════════
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
        } catch (e: Exception) { /* ignore */ }
        println("🛑 [MQTT] Disconnected")
    }

    fun isConnected(): Boolean = mqttClient?.isConnected == true
}