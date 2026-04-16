// ViewModels/VehicleViewModel.swift
import Foundation
import SwiftUI
import Combine

class VehicleViewModel: ObservableObject, MQTTManagerDelegate {

    // ── MQTT ──────────────────────────────────────────────────────
    private let mqtt = MQTTManager.shared

    // ── @Published → SwiftUI auto re-renders on change ────────────
    @Published var connectionState : ConnectionState = .disconnected
    @Published var latestData      : VehicleData?    = nil
    @Published var history         : [VehicleData]   = []
    @Published var rawMessages     : [RawMessage]    = []
    @Published var totalReceived   : Int             = 0

    // ── Init ──────────────────────────────────────────────────────
    init() {
        mqtt.delegate = self
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTIONS — Called from Views
    // ══════════════════════════════════════════════════════════════

    func connect() {
        connectionState = .connecting
        mqtt.connect()
    }

    func disconnect() {
        mqtt.disconnect()
        connectionState = .disconnected
    }

    func toggleConnection() {
        connectionState.isConnected ? disconnect() : connect()
    }

    func clearAll() {
        history.removeAll()
        rawMessages.removeAll()
        totalReceived = 0
    }

    // ══════════════════════════════════════════════════════════════
    //  MQTTManagerDelegate — Callbacks from MQTTManager
    // ══════════════════════════════════════════════════════════════

    // ✅ Connected
    func onConnected() {
        DispatchQueue.main.async {
            self.connectionState = .connected
        }
    }

    // 🔴 Disconnected
    func onDisconnected(reason: String?) {
        DispatchQueue.main.async {
            self.connectionState = reason != nil
                ? .failed(reason!)
                : .disconnected
        }
    }

    // 📥 New vehicle data
    func onVehicleData(_ data: VehicleData) {
        DispatchQueue.main.async {
            self.latestData    = data
            self.totalReceived += 1
            // Newest first — keep max 50
            self.history.insert(data, at: 0)
            if self.history.count > 50 {
                self.history.removeLast()
            }
        }
    }

    // 📥 Raw JSON (for debug)
    func onRawMessage(topic: String, payload: String) {
        DispatchQueue.main.async {
            let msg = RawMessage(topic: topic, payload: payload)
            self.rawMessages.insert(msg, at: 0)
            if self.rawMessages.count > 30 {
                self.rawMessages.removeLast()
            }
        }
    }

    // ── Computed Stats ────────────────────────────────────────────
    var avgSpeed: Int {
        guard !history.isEmpty else { return 0 }
        return history.map(\.speed).reduce(0, +) / history.count
    }

    var maxSpeed: Int { history.map(\.speed).max() ?? 0 }
    var maxRPM  : Int { history.map(\.rpm).max()   ?? 0 }
}
