// MQTT/MQTTManager.swift
import Foundation
import CocoaMQTT

// ─── Delegate Protocol ─────────────────────────────────────────────
protocol MQTTManagerDelegate: AnyObject {
    func onConnected()
    func onDisconnected(reason: String?)
    func onVehicleData(_ data: VehicleData)
    func onRawMessage(topic: String, payload: String)
}

// ─── MQTT Manager ──────────────────────────────────────────────────
class MQTTManager: NSObject {

    // ── Singleton ─────────────────────────────────────────────────
    static let shared = MQTTManager()

    // ── Delegate → ViewModel ──────────────────────────────────────
    weak var delegate: MQTTManagerDelegate?

    // ── CocoaMQTT Client ──────────────────────────────────────────
    private var client: CocoaMQTT?

    // ── Broker Config ─────────────────────────────────────────────
    // iOS Simulator  →  "127.0.0.1"
    // Real iPhone    →  Your Mac WiFi IP shown in broker startup log
    //   e.g. "192.168.1.100"
    // Override: Xcode → Edit Scheme → Run → Environment Variables → MQTT_HOST
    // ── Server IP shared by Android + iOS ────────────────────────
    static var defaultHost: String {
        ProcessInfo.processInfo.environment["MQTT_HOST"] ?? "192.168.0.101"
    }

    private let port: UInt16 = 1883

    // ── Topics ────────────────────────────────────────────────────
    private let topicResponse = "vehicle/response"

    // ── Private init (Singleton) ──────────────────────────────────
    private override init() {}

    // ══════════════════════════════════════════════════════════════
    //  CONNECT  — host defaults to MQTT_HOST env var or "127.0.0.1"
    // ══════════════════════════════════════════════════════════════
    func connect(host: String = MQTTManager.defaultHost) {
        let clientID = "ios-\(UUID().uuidString.prefix(6))"

        client = CocoaMQTT(clientID: clientID, host: host, port: port)
        client?.username        = ""
        client?.password        = ""
        client?.keepAlive       = 60
        client?.cleanSession    = true
        client?.autoReconnect   = true
        client?.autoReconnectTimeInterval = 3
        client?.delegate        = self

        print("🔄 [MQTT] Connecting → \(host):\(port)")
        client?.connect()
    }

    // ══════════════════════════════════════════════════════════════
    //  DISCONNECT
    // ══════════════════════════════════════════════════════════════
    func disconnect() {
        client?.disconnect()
        print("🛑 [MQTT] Disconnected")
    }

    // ══════════════════════════════════════════════════════════════
    //  SUBSCRIBE — Called after connection success
    // ══════════════════════════════════════════════════════════════
    private func subscribe() {
        client?.subscribe(topicResponse, qos: .qos1)
        print("📡 [MQTT] Subscribed → \(topicResponse)")
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PARSE JSON → VehicleData
    // ═══════════════════════════════════════════════════════════════════
    private func parse(_ json: String) {
        guard let data = json.data(using: .utf8) else {
            print("❌ [MQTT] Failed to convert payload to data")
            return
        }
        do {
            let vehicle = try JSONDecoder().decode(VehicleData.self, from: data)
            print("✅ [MQTT] Successfully parsed VehicleData: speed=\(vehicle.speed) rpm=\(vehicle.rpm)")
            delegate?.onVehicleData(vehicle)
        } catch {
            print("❌ [MQTT] Parse error: \(error.localizedDescription)")
            print("❌ [MQTT] Failed JSON: \(json)")
        }
    }
}

// ─── CocoaMQTT Delegate ────────────────────────────────────────────
extension MQTTManager: CocoaMQTTDelegate {

    // ✅ Connected
    func mqtt(_ mqtt: CocoaMQTT, didConnectAck ack: CocoaMQTTConnAck) {
        if ack == .accept {
            subscribe()
            delegate?.onConnected()
            print("✅ [MQTT] Connected! ID: \(mqtt.clientID)")
        } else {
            delegate?.onDisconnected(reason: "Rejected: \(ack)")
            print("❌ [MQTT] Rejected: \(ack)")
        }
    }

    // 📥 Message Received
    func mqtt(_ mqtt: CocoaMQTT, didReceiveMessage message: CocoaMQTTMessage, id: UInt16) {
        guard let payload = message.string else { return }
        let topic = message.topic

        print("📥 [MQTT] [\(topic)] \(payload)")

        // Notify debug view
        delegate?.onRawMessage(topic: topic, payload: payload)

        // Parse vehicle data
        if topic == topicResponse {
            parse(payload)
        }
    }

    // ✅ Subscribed
    func mqtt(_ mqtt: CocoaMQTT,
              didSubscribeTopics success: NSDictionary,
              failed: [String]) {
        print("📡 [MQTT] Topics OK: \(success.allKeys)")
    }

    // 🔴 Disconnected
    func mqttDidDisconnect(_ mqtt: CocoaMQTT, withError err: Error?) {
        delegate?.onDisconnected(reason: err?.localizedDescription)
        print("🔴 [MQTT] Disconnected: \(err?.localizedDescription ?? "OK")")
    }

    // ── Required stubs ────────────────────────────────────────────
    func mqtt(_ mqtt: CocoaMQTT, didPublishMessage message: CocoaMQTTMessage, id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didPublishAck id: UInt16) {}
    func mqtt(_ mqtt: CocoaMQTT, didUnsubscribeTopics topics: [String]) {}
    func mqttDidPing(_ mqtt: CocoaMQTT) {}
    func mqttDidReceivePong(_ mqtt: CocoaMQTT) {}
}
