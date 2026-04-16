package org.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.smarthome.models.ConnectionState
import org.smarthome.models.GPSPayload
import org.smarthome.models.OBDPayload
import org.smarthome.models.SentMessage
import org.smarthome.models.VehicleResponse
import org.smarthome.mqtt.MQTTListener
import org.smarthome.mqtt.MQTTManager
import java.util.UUID

class VehicleViewModel : ViewModel(), MQTTListener {

    private val mqtt = MQTTManager.shared

    // ══════════════════════════════════════════════════════════════
    //  STATE — All UI state as StateFlow
    //  Views collect these and auto-update when they change
    // ══════════════════════════════════════════════════════════════

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap Connect to start")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _sentMessages = MutableStateFlow<List<SentMessage>>(emptyList())
    val sentMessages: StateFlow<List<SentMessage>> = _sentMessages.asStateFlow()

    private val _latestResponse = MutableStateFlow<VehicleResponse?>(null)
    val latestResponse: StateFlow<VehicleResponse?> = _latestResponse.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _totalSent = MutableStateFlow(0)
    val totalSent: StateFlow<Int> = _totalSent.asStateFlow()

    // ── Broker Host — same server IP used by Android + iOS ───────
    // Server machine IP: 192.168.0.101
    private val _brokerHost = MutableStateFlow("192.168.0.101")
    val brokerHost: StateFlow<String> = _brokerHost.asStateFlow()

    // ── GPS coordinates (editable from CommandScreen) ─────────────
    private val _lat = MutableStateFlow(19.0760)
    val lat: StateFlow<Double> = _lat.asStateFlow()

    private val _lng = MutableStateFlow(72.8777)
    val lng: StateFlow<Double> = _lng.asStateFlow()

    // ── Unique device ID ──────────────────────────────────────────
    val deviceId = "android_${UUID.randomUUID().toString().take(6)}"

    // ── Auto simulation coroutine job ─────────────────────────────
    private var simulationJob: Job? = null

    init {
        mqtt.listener = this
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTIONS — Called from UI
    // ══════════════════════════════════════════════════════════════

    fun updateBrokerHost(host: String) { _brokerHost.value = host }
    fun updateLat(value: String)  { value.toDoubleOrNull()?.let { _lat.value = it } }
    fun updateLng(value: String)  { value.toDoubleOrNull()?.let { _lng.value = it } }

    // ── Connect ───────────────────────────────────────────────────
    fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        _statusMessage.value   = "Connecting to ${_brokerHost.value}:1883..."

        // ⚠️ Paho connect() is BLOCKING → must use Dispatchers.IO
        viewModelScope.launch(Dispatchers.IO) {
            mqtt.connect(
                host     = _brokerHost.value,
                port     = 1883,
                deviceId = deviceId
            )
        }
    }

    // ── Disconnect ────────────────────────────────────────────────
    fun disconnect() {
        stopSimulation()
        mqtt.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
        _statusMessage.value   = "Disconnected"
    }

    fun toggleConnection() {
        if (_connectionState.value.isConnected) disconnect() else connect()
    }

    // ── Send RPM Command (OBD PID 010C) ───────────────────────────
    fun sendRPMCommand() {
        if (!checkConnected()) return
        val payload = OBDPayload(command = "010C", deviceId = deviceId).toJson()
        viewModelScope.launch(Dispatchers.IO) {
            mqtt.publish(MQTTManager.TOPIC_COMMANDS, payload)
        }
    }

    // ── Send Speed Command (OBD PID 010D) ─────────────────────────
    fun sendSpeedCommand() {
        if (!checkConnected()) return
        val payload = OBDPayload(command = "010D", deviceId = deviceId).toJson()
        viewModelScope.launch(Dispatchers.IO) {
            mqtt.publish(MQTTManager.TOPIC_COMMANDS, payload)
        }
    }

    // ── Send GPS ──────────────────────────────────────────────────
    fun sendGPS() {
        if (!checkConnected()) return
        val payload = GPSPayload(
            lat = _lat.value, lng = _lng.value, deviceId = deviceId
        ).toJson()
        viewModelScope.launch(Dispatchers.IO) {
            mqtt.publish(MQTTManager.TOPIC_GPS, payload)
        }
    }

    // ── Start Auto Simulation ─────────────────────────────────────
    //  Sends 010C + 010D + GPS every ~3 seconds in a loop
    fun startSimulation() {
        if (!checkConnected()) return

        _isSimulating.value = true
        simulationJob = viewModelScope.launch {
            var cycle = 1
            while (isActive) {
                _statusMessage.value = "🔄 Simulating — cycle #$cycle"

                // 1. Send RPM
                sendRPMCommand()
                delay(350)

                // 2. Send Speed
                sendSpeedCommand()
                delay(350)

                // 3. Send GPS (with small random movement)
                _lat.value = 19.0760 + (Math.random() * 0.004 - 0.002)
                _lng.value = 72.8777 + (Math.random() * 0.004 - 0.002)
                sendGPS()

                delay(3000)   // Wait 3s before next cycle
                cycle++
            }
        }
    }

    // ── Stop Auto Simulation ──────────────────────────────────────
    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _isSimulating.value  = false
        _statusMessage.value = "Simulation stopped"
    }

    // ── Clear log ─────────────────────────────────────────────────
    fun clearLog() {
        _sentMessages.value = emptyList()
        _totalSent.value    = 0
    }

    // ── Helper ────────────────────────────────────────────────────
    private fun checkConnected(): Boolean {
        if (!mqtt.isConnected()) {
            _statusMessage.value = "⚠️ Not connected! Tap Connect first."
            return false
        }
        return true
    }

    // ══════════════════════════════════════════════════════════════
    //  MQTTListener — Callbacks FROM MQTTManager
    //  StateFlow.value setter is thread-safe → can be called from any thread
    // ══════════════════════════════════════════════════════════════

    override fun onConnected() {
        _connectionState.value = ConnectionState.CONNECTED
        _statusMessage.value   = "✅ Connected to ${_brokerHost.value}:1883"
        println("✅ [VM] Connected")
    }

    override fun onDisconnected(reason: String?) {
        // If reason is non-null it's an unexpected drop — show CONNECTING (auto-reconnect is on)
        // If reason is null it was a deliberate disconnect()
        if (reason != null) {
            _connectionState.value = ConnectionState.CONNECTING
            _statusMessage.value   = "⚠️ Connection lost — reconnecting… ($reason)"
            println("⚠️ [VM] Unexpected disconnect, auto-reconnect in progress")
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
            _statusMessage.value   = "Disconnected"
            _isSimulating.value    = false
            simulationJob?.cancel()
        }
    }

    // Called when we receive data on vehicle/response
    override fun onMessageReceived(topic: String, payload: String) {
        if (topic == MQTTManager.TOPIC_RESPONSE) {
            parseResponse(payload)
        }
    }

    // Called after each successful publish
    override fun onPublishSuccess(topic: String, payload: String) {
        val msg = SentMessage(topic = topic, payload = payload)
        // Newest first, max 50 entries
        _sentMessages.value = listOf(msg) + _sentMessages.value.take(49)
        _totalSent.value += 1
    }

    override fun onError(error: String) {
        _connectionState.value = ConnectionState.FAILED
        _statusMessage.value   = "❌ Error: $error"
    }

    // ── Parse JSON from vehicle/response ──────────────────────────
    private fun parseResponse(json: String) {
        try {
            val obj = JSONObject(json)
            _latestResponse.value = VehicleResponse(
                speed     = obj.optInt("speed", 0),
                rpm       = obj.optInt("rpm", 0),
                lat       = obj.optDouble("lat", 0.0),
                lng       = obj.optDouble("lng", 0.0),
                timestamp = obj.optString("timestamp", ""),
                deviceId  = obj.optString("deviceId", "")
            )
        } catch (e: Exception) {
            println("❌ [VM] Response parse error: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        mqtt.disconnect()
    }
}