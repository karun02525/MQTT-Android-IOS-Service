package org.smarthome// src/main/kotlin/EmbeddedBroker.kt

import io.moquette.broker.Server
import io.moquette.broker.config.IConfig
import io.moquette.broker.config.MemoryConfig
import java.net.BindException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Properties

object EmbeddedBroker {

    private val server = Server()

    fun start(port: Int = 1883) {
        val bindHost = "0.0.0.0"
        val props = Properties().apply {
            // Bind to all interfaces so simulators/emulators/devices can connect.
            setProperty(IConfig.HOST_PROPERTY_NAME, bindHost)
            setProperty(IConfig.PORT_PROPERTY_NAME, port.toString())
            setProperty(IConfig.ALLOW_ANONYMOUS_PROPERTY_NAME, "true")
        }

        try {
            server.startServer(MemoryConfig(props))

            val machineHostName = runCatching { InetAddress.getLocalHost().hostName }
                .getOrDefault("unknown-host")
            val lanIps = detectLanIpv4Addresses()
            val realDeviceHost = lanIps.firstOrNull() ?: "<your-mac-lan-ip>"

            println(
                """
                ┌──────────────────────────────────────────────────────┐
                │  MQTT BROKER STARTED                                │
                │  Bind Host : $bindHost                              │
                │  Port      : $port                                  │
                │  Host Name : $machineHostName                       │
                │  iOS (sim) : localhost                              │
                │  Android em: 10.0.2.2                               │
                │  Real phone: $realDeviceHost                        │
                └──────────────────────────────────────────────────────┘
                """.trimIndent()
            )

            if (lanIps.size > 1) {
                println("[BROKER] Other LAN IPs: ${lanIps.drop(1).joinToString()}")
            }
        } catch (e: Exception) {
            if (e is BindException || e.cause is BindException) {
                println(
                    """
                    ❌ Broker start failed: Port $port is already in use.
                    Try one of these:
                    1) Stop old process: lsof -nP -iTCP:$port -sTCP:LISTEN
                       then: kill -9 <PID>
                    2) Start with another port using env:
                       BROKER_PORT=1884 ./gradlew :server:run
                    """.trimIndent()
                )
            }
            println("❌ Broker start failed: ${e.message}")
            throw e
        }
    }

    private fun detectLanIpv4Addresses(): List<String> {
        return runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { iface -> Collections.list(iface.inetAddresses).asSequence() }
                .mapNotNull { addr ->
                    addr.hostAddress?.takeIf { host -> !host.contains(":") && host != "127.0.0.1" }
                }
                .distinct()
                .sorted()
                .toList()
        }.getOrElse { emptyList() }
    }

    fun stop() {
        server.stopServer()
        println("🛑 [BROKER] Stopped")
    }
}