package org.smarthome// src/main/kotlin/EmbeddedBroker.kt

import io.moquette.broker.Server
import io.moquette.broker.config.IConfig
import io.moquette.broker.config.MemoryConfig
import java.util.Properties

object EmbeddedBroker {

    private val server = Server()

    fun start(port: Int = 1883) {
        val props = Properties().apply {
            // Broker host
            setProperty(IConfig.HOST_PROPERTY_NAME, "localhost")
            // Broker port
            setProperty(IConfig.PORT_PROPERTY_NAME, port.toString())
            // Allow connect without username/password
            setProperty(IConfig.ALLOW_ANONYMOUS_PROPERTY_NAME, "true")
            // Store messages in memory (no file system)
            setProperty(IConfig.PERSISTENT_QUEUE_TYPE_PROPERTY_NAME, "in_memory")
        }

        try {
            server.startServer(MemoryConfig(props))
            println("""
                ┌────────────────────────────────────────┐
                │  ✅ MQTT BROKER STARTED                 │
                │  Host : localhost                       │
                │  Port : $port                           │
                │  Mode : Anonymous allowed               │
                │  Store: In-Memory                      │
                └────────────────────────────────────────┘
            """.trimIndent())
        } catch (e: Exception) {
            println("❌ Broker start failed: ${e.message}")
            throw e
        }
    }

    fun stop() {
        server.stopServer()
        println("🛑 [BROKER] Stopped")
    }
}