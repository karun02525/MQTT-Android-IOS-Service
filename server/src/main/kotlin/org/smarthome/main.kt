package org.smarthome// src/main/kotlin/Main.kt

import kotlinx.coroutines.*

fun main(): Unit = runBlocking {
    val brokerPort = System.getenv("BROKER_PORT")?.toIntOrNull() ?: 1883
    val brokerUrl = "tcp://localhost:$brokerPort"
    val bridge = VehicleBridge(brokerUrl = brokerUrl)

    println("""
        ╔════════════════════════════════════════════════════╗
        ║      🚗 MQTT VEHICLE SYSTEM — KOTLIN ONLY         ║
        ║         No Docker  •  No External Broker          ║
        ╠════════════════════════════════════════════════════╣
        ║  Flow:                                            ║
        ║  Android → vehicle/commands  → Ktor Bridge        ║
        ║  Android → vehicle/gps       → Ktor Bridge        ║
        ║  Ktor Bridge → vehicle/response → iOS             ║
        ╚════════════════════════════════════════════════════╝
    """.trimIndent())

    // ══════════════════════════════════════════════════════
    // PHASE 1: Start Broker
    // ══════════════════════════════════════════════════════
    println("\n━━━ PHASE 1: Start MQTT Broker (port=$brokerPort) ━━━")
    EmbeddedBroker.start(port = brokerPort)
    delay(800) // Wait for broker to fully initialize

    // ══════════════════════════════════════════════════════
    // PHASE 2: Start Ktor Bridge (Android -> iOS)
    // ══════════════════════════════════════════════════════
    println("\n━━━ PHASE 2: Start Ktor Bridge ━━━")
    bridge.connectAndProcess()
    delay(500)

    // ══════════════════════════════════════════════════════
    // RUNTIME MODE: wait for Android commands, iOS subscribes to response
    // ══════════════════════════════════════════════════════
    println("""
        
        ════════════════════════════════════════════
        ✅ READY
        Android publishes: vehicle/commands + vehicle/gps
        iOS listens on : vehicle/response
        ════════════════════════════════════════════
    """.trimIndent())

    Runtime.getRuntime().addShutdownHook(Thread {
        println("\n🧹 Shutdown: disconnecting bridge + stopping broker...")
        runCatching { bridge.disconnect() }
        runCatching { EmbeddedBroker.stop() }
    })

    awaitCancellation()
}