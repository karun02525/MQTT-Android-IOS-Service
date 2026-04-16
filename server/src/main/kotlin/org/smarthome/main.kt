package org.smarthome// src/main/kotlin/Main.kt

import kotlinx.coroutines.*

fun main() = runBlocking {

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
    println("\n━━━ PHASE 1: Start MQTT Broker ━━━")
    EmbeddedBroker.start()
    delay(800) // Wait for broker to fully initialize

    // ══════════════════════════════════════════════════════
    // PHASE 2: Start iOS Subscriber (listen FIRST)
    // ══════════════════════════════════════════════════════
    println("\n━━━ PHASE 2: Start iOS Subscriber ━━━")
    val subscriber = VehicleSubscriber()
    subscriber.connectAndListen()
    delay(500)

    // ══════════════════════════════════════════════════════
    // PHASE 3: Start Ktor Bridge (process messages)
    // ══════════════════════════════════════════════════════
    println("\n━━━ PHASE 3: Start Ktor Bridge ━━━")
    val bridge = VehicleBridge()
    bridge.connectAndProcess()
    delay(500)

    // ══════════════════════════════════════════════════════
    // PHASE 4: Start Android Publisher
    // ══════════════════════════════════════════════════════
    println("\n━━━ PHASE 4: Start Android Publisher ━━━")
    val publisher = VehiclePublisher()
    publisher.connect()
    delay(500)

    // ══════════════════════════════════════════════════════
    // TEST 1: Manual — Send each command one by one
    // ══════════════════════════════════════════════════════
    println("""
        
        ════════════════════════════════════════════
        🧪 TEST 1 — Manual OBD + GPS Commands
        ════════════════════════════════════════════
    """.trimIndent())

    // Android sends RPM request
    publisher.sendRPMCommand()
    delay(500)

    // Android sends Speed request
    publisher.sendSpeedCommand()
    delay(500)

    // Android sends GPS
    // ⚠️ Response publishes ONLY after all 3 arrive (RPM + Speed + GPS)
    publisher.sendGPS(lat = 19.0760, lng = 72.8777)
    delay(1000) // Wait for iOS to receive

    // ══════════════════════════════════════════════════════
    // TEST 2: Auto Simulation — CONTINUOUS (Until interrupted)
    // ══════════════════════════════════════════════════════
    println("""
        
        ════════════════════════════════════════════
        🔄 CONTINUOUS SIMULATION (Press Ctrl+C to stop)
        ════════════════════════════════════════════
    """.trimIndent())

    var cycleCount = 0
    while (true) {
        cycleCount++
        println("\n  ── Cycle $cycleCount ──")

        publisher.sendRPMCommand()
        delay(300)

        publisher.sendSpeedCommand()
        delay(300)

        // Simulate moving vehicle (slight GPS variation each cycle)
        publisher.sendGPS(
            lat = 19.0760 + (Math.random() * 0.004 - 0.002),
            lng = 72.8777 + (Math.random() * 0.004 - 0.002)
        )
        delay(2000) // Wait 2 seconds between cycles
    }

    // ══════════════════════════════════════════════════════
    // CLEANUP (only reached on interrupt or shutdown)
    // ══════════════════════════════════════════════════════
    // println("""
    //
    //     ════════════════════════════════════════════
    //     🧹 Cleanup
    //     ════════════════════════════════════════════
    // """.trimIndent())
    //
    // publisher.disconnect()
    // delay(200)
    // bridge.disconnect()
    // delay(200)
    // subscriber.disconnect()
    // delay(200)
    // EmbeddedBroker.stop()
    //
    // println("""
    //
    //     ╔════════════════════════════════════════════╗
    //     ║         ✅ ALL TESTS COMPLETE!             ║
    //     ╚════════════════════════════════════════════╝
    // """.trimIndent())
}