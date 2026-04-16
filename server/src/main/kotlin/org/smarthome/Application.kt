package org.smarthome

import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import io.moquette.interception.AbstractInterceptHandler
import io.moquette.interception.messages.InterceptPublishMessage
import io.netty.buffer.Unpooled
import io.netty.handler.codec.mqtt.*
import java.nio.charset.StandardCharsets
import java.util.*

fun main() {

    val mqttBroker = Server()

    // 🔧 1. Broker Configuration
    val configProps = Properties().apply {
        setProperty("port", "1883")
        setProperty("host", "0.0.0.0")
        setProperty("allow_anonymous", "true")
    }

    // 🔁 2. Interceptor (Core Logic)
    val interceptor = object : AbstractInterceptHandler() {

        override fun getID(): String = "VehicleInterceptor"

        override fun onPublish(msg: InterceptPublishMessage) {

            val topic = msg.topicName
            val payload = msg.payload.toString(StandardCharsets.UTF_8)

            println("📩 Received on [$topic]: $payload")

            // 🚗 Handle Android commands
            if (topic == "android/commands") {

                val response = when (payload.trim()) {
                    "010C" -> """{"speed": 62, "rpm": 2100, "lat": 19.0760, "lng": 72.8777}"""
                    "010D" -> """{"speed": 45, "rpm": 1500, "lat": 19.0760, "lng": 72.8777}"""
                    else -> """{"error": "unknown command"}"""
                }

                println("📤 Sending to ios/telemetry: $response")

                publishMessage(mqttBroker, "ios/telemetry", response)
            }
        }

        override fun onSessionLoopError(error: Throwable?) {
            println("⚠️ MQTT Error: ${error?.message}")
            error?.printStackTrace()
        }
    }

    // ▶️ 3. Start Broker
    mqttBroker.startServer(MemoryConfig(configProps), listOf(interceptor))

    println("🚀 MQTT Broker running at tcp://192.168.0.101:1883")

    // 🛑 Graceful Shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Stopping MQTT Broker...")
        mqttBroker.stopServer()
    })
}


// 🔥 Helper function to publish message
fun publishMessage(broker: Server, topic: String, json: String) {

    val payloadBuffer = Unpooled.copiedBuffer(json.toByteArray(StandardCharsets.UTF_8))

    val fixedHeader = MqttFixedHeader(
        MqttMessageType.PUBLISH,
        false,
        MqttQoS.AT_MOST_ONCE,
        false,
        0
    )

    val variableHeader = MqttPublishVariableHeader(topic, 0)

    val message = MqttPublishMessage(
        fixedHeader,
        variableHeader,
        payloadBuffer
    )

    broker.internalPublish(message, "server")
}