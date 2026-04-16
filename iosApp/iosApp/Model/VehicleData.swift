// Models/VehicleData.swift
import Foundation
import SwiftUI

// ─── Main Model ────────────────────────────────────────────────────
// This is the JSON we receive from Ktor → vehicle/response topic
//
//  {
//    "speed"     : 62,
//    "rpm"       : 2100,
//    "lat"       : 19.0760,
//    "lng"       : 72.8777,
//    "timestamp" : "2026-04-15T17:12:00",
//    "deviceId"  : "device_001"
//  }

struct VehicleData: Codable, Identifiable, Equatable {

    // UUID — for SwiftUI List (not in JSON)
    var id = UUID()

    // ── JSON Fields ───────────────────────────────────────────────
    let speed     : Int      // km/h   from OBD PID 010D
    let rpm       : Int      // RPM    from OBD PID 010C
    let lat       : Double   // GPS Latitude
    let lng       : Double   // GPS Longitude
    let timestamp : String   // "2026-04-15T17:12:00"
    let deviceId  : String   // "device_001"

    // ── Skip 'id' during JSON decode ──────────────────────────────
    enum CodingKeys: String, CodingKey {
        case speed, rpm, lat, lng, timestamp, deviceId
    }

    // ── Equatable ─────────────────────────────────────────────────
    static func == (lhs: VehicleData, rhs: VehicleData) -> Bool {
        lhs.timestamp == rhs.timestamp && lhs.deviceId == rhs.deviceId
    }

    // ── Display Helpers ───────────────────────────────────────────

    // "2026-04-15T17:12:00" → "17:12:00"
    var timeOnly: String {
        timestamp.split(separator: "T").last.map(String.init) ?? timestamp
    }

    var latFormatted: String { String(format: "%.5f°", lat) }
    var lngFormatted: String { String(format: "%.5f°", lng) }

    // ── Speed Color ───────────────────────────────────────────────
    var speedColor: Color {
        switch speed {
        case 0..<40  : return .green
        case 40..<80 : return .orange
        default      : return .red
        }
    }

    // ── RPM Color ─────────────────────────────────────────────────
    var rpmColor: Color {
        switch rpm {
        case 0..<1500    : return .blue
        case 1500..<3000 : return .green
        default          : return .red
        }
    }

    // ── Speed Label ───────────────────────────────────────────────
    var speedLabel: String {
        switch speed {
        case 0..<40  : return "Slow"
        case 40..<80 : return "Normal"
        default      : return "Fast"
        }
    }

    // ── RPM Label ─────────────────────────────────────────────────
    var rpmLabel: String {
        switch rpm {
        case 0..<1500    : return "Idle"
        case 1500..<3000 : return "Normal"
        default          : return "High Rev"
        }
    }
}

// ─── Connection State ──────────────────────────────────────────────
enum ConnectionState {
    case disconnected
    case connecting
    case connected
    case failed(String)

    var label: String {
        switch self {
        case .disconnected   : return "Disconnected"
        case .connecting     : return "Connecting..."
        case .connected      : return "Connected"
        case .failed(let e)  : return "Failed: \(e)"
        }
    }

    var color: Color {
        switch self {
        case .disconnected   : return .gray
        case .connecting     : return .orange
        case .connected      : return .green
        case .failed         : return .red
        }
    }

    var isConnected: Bool {
        if case .connected = self { return true }
        return false
    }
}

// ─── Raw MQTT Message (for Debug tab) ─────────────────────────────
struct RawMessage: Identifiable {
    let id      = UUID()
    let topic   : String
    let payload : String
    let time    : Date = .now

    var timeString: String {
        time.formatted(date: .omitted, time: .standard)
    }
}
