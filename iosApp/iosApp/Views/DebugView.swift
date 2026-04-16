// Views/DebugView.swift
import SwiftUI

struct DebugView: View {
    @ObservedObject var vm: VehicleViewModel

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                VStack(spacing: 0) {
                    // ── Status Header ────────────────────────────────
                    StatusHeader(vm: vm)

                    Divider()
                        .background(.gray.opacity(0.3))

                    // ── Messages List ────────────────────────────────
                    if vm.rawMessages.isEmpty {
                        ContentUnavailableView(
                            "No Messages",
                            systemImage: "terminal",
                            description: Text("Raw MQTT messages\nwill appear here")
                        )
                        .colorScheme(.dark)
                    } else {
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 8) {
                                ForEach(vm.rawMessages) { msg in
                                    DebugRow(msg: msg)
                                }
                            }
                            .padding()
                        }
                    }
                }
            }
            .navigationTitle("🐛 Debug Console")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Clear") { vm.clearAll() }
                        .foregroundStyle(.green)
                        .disabled(vm.rawMessages.isEmpty)
                }
            }
        }
    }
}

// ─── Status Header (Connection + Stats) ────────────────────────────
struct StatusHeader: View {
    @ObservedObject var vm: VehicleViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Circle()
                    .fill(vm.connectionState.color)
                    .frame(width: 10, height: 10)

                VStack(alignment: .leading, spacing: 2) {
                    Text(vm.connectionState.label)
                        .font(.subheadline).fontWeight(.semibold)
                        .foregroundStyle(.white)
                    Text("127.0.0.1:1883")
                        .font(.caption2).foregroundStyle(.gray)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("Messages: \(vm.rawMessages.count)")
                        .font(.caption).foregroundStyle(.green)
                    Text("Parsed: \(vm.totalReceived)")
                        .font(.caption).foregroundStyle(.blue)
                }
            }

            if let data = vm.latestData {
                Divider()
                    .background(.gray.opacity(0.3))

                HStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Latest Data:")
                            .font(.caption).foregroundStyle(.gray)
                        Text("🚗 \(data.speed) km/h")
                            .font(.caption).foregroundStyle(.green)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("RPM:")
                            .font(.caption).foregroundStyle(.gray)
                        Text("\(data.rpm)")
                            .font(.caption).foregroundStyle(.yellow)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Device:")
                            .font(.caption).foregroundStyle(.gray)
                        Text(data.deviceId)
                            .font(.caption2).foregroundStyle(.purple)
                    }

                    Spacer()
                }
            }
        }
        .padding()
        .background(.black.opacity(0.3))
    }
}

// ─── Single Console Row ────────────────────────────────────────────
struct DebugRow: View {
    let msg: RawMessage

    // Format JSON nicely
    var formattedJSON: String {
        guard
            let data = msg.payload.data(using: .utf8),
            let obj  = try? JSONSerialization.jsonObject(with: data),
            let pretty = try? JSONSerialization.data(
                withJSONObject: obj,
                options: .prettyPrinted
            ),
            let str = String(data: pretty, encoding: .utf8)
        else {
            return msg.payload
        }
        return str
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {

            // ── Header row ────────────────────────────────────────
            HStack(spacing: 8) {
                Text("▶")
                    .font(.caption2).foregroundStyle(.green)

                // Topic pill
                Text(msg.topic)
                    .font(.caption).fontWeight(.bold)
                    .foregroundStyle(.black)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(.green)
                    .clipShape(Capsule())

                Spacer()

                // Timestamp
                Text(msg.timeString)
                    .font(.caption2)
                    .foregroundStyle(.gray)
            }

            // ── JSON Payload ──────────────────────────────────────
            Text(formattedJSON)
                .font(.system(.caption, design: .monospaced))
                .foregroundStyle(.green)
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white.opacity(0.05))
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }
}

#Preview {
    DebugView(vm: VehicleViewModel())
}
