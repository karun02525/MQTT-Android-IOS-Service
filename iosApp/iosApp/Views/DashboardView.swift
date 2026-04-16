// Views/DashboardView.swift
import SwiftUI

struct DashboardView: View {
    @ObservedObject var vm: VehicleViewModel

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {

                    // ── Connection Banner ─────────────────────────
                    ConnectionBanner(vm: vm)

                    if let data = vm.latestData {

                        // ── Speed Gauge ───────────────────────────
                        SpeedGauge(speed: data.speed, color: data.speedColor)

                        // ── RPM + Device Cards ────────────────────
                        HStack(spacing: 12) {
                            InfoCard(
                                icon   : "⚙️",
                                title  : "RPM",
                                value  : "\(data.rpm)",
                                badge  : data.rpmLabel,
                                color  : data.rpmColor
                            )
                            InfoCard(
                                icon   : "📱",
                                title  : "Device",
                                value  : data.deviceId,
                                badge  : "Online",
                                color  : .purple
                            )
                        }

                        // ── GPS Card ──────────────────────────────
                        GPSCard(lat: data.lat, lng: data.lng)

                        // ── Session Stats ─────────────────────────
                        SessionStatsCard(vm: vm)

                    } else {
                        // ── Waiting ───────────────────────────────
                        WaitingCard(isConnected: vm.connectionState.isConnected)
                    }
                }
                .padding()
            }
            .navigationTitle("🚗 Vehicle Monitor")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}

// ─── Connection Banner ─────────────────────────────────────────────
struct ConnectionBanner: View {
    @ObservedObject var vm: VehicleViewModel
    @State private var pulse = false

    var body: some View {
        HStack(spacing: 10) {

            // Pulsing dot
            Circle()
                .fill(vm.connectionState.color)
                .frame(width: 11, height: 11)
                .overlay(
                    Circle()
                        .stroke(vm.connectionState.color.opacity(0.35), lineWidth: 5)
                        .scaleEffect(pulse ? 1.8 : 1.0)
                )
                .onAppear {
                    withAnimation(.easeInOut(duration: 1.1).repeatForever()) {
                        pulse = true
                    }
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(vm.connectionState.label)
                    .font(.subheadline).fontWeight(.semibold)
                Text("127.0.0.1:1883 → vehicle/response")
                    .font(.caption2).foregroundStyle(.secondary)
            }

            Spacer()

            // Connect / Disconnect
            Button(vm.connectionState.isConnected ? "Disconnect" : "Connect") {
                vm.toggleConnection()
            }
            .buttonStyle(.bordered)
            .tint(vm.connectionState.isConnected ? .red : .blue)
            .controlSize(.small)
        }
        .padding(12)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// ─── Speed Gauge ───────────────────────────────────────────────────
struct SpeedGauge: View {
    let speed : Int
    let color : Color

    // progress: 0.0 → 1.0  (max 200 km/h)
    var progress: Double { Double(min(speed, 200)) / 200.0 }

    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                // Grey background arc
                Circle()
                    .trim(from: 0.1, to: 0.9)
                    .stroke(.gray.opacity(0.15), lineWidth: 22)
                    .rotationEffect(.degrees(90))

                // Colored speed arc
                Circle()
                    .trim(from: 0.1, to: 0.1 + 0.8 * progress)
                    .stroke(color, style: StrokeStyle(lineWidth: 22, lineCap: .round))
                    .rotationEffect(.degrees(90))
                    .animation(.spring(response: 0.7), value: speed)

                // Center content
                VStack(spacing: 2) {
                    Text("\(speed)")
                        .font(.system(size: 56, weight: .bold, design: .rounded))
                        .foregroundStyle(color)
                        .contentTransition(.numericText())
                        .animation(.spring(), value: speed)

                    Text("km / h")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 210, height: 210)
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(.background)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.06), radius: 10, y: 4)
    }
}

// ─── Info Card ─────────────────────────────────────────────────────
struct InfoCard: View {
    let icon   : String
    let title  : String
    let value  : String
    let badge  : String
    let color  : Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(icon).font(.title2)
                Spacer()
                Text(badge)
                    .font(.caption2).fontWeight(.semibold)
                    .padding(.horizontal, 7).padding(.vertical, 3)
                    .background(color.opacity(0.12))
                    .foregroundStyle(color)
                    .clipShape(Capsule())
            }
            Text(title)
                .font(.caption).foregroundStyle(.secondary)
            Text(value)
                .font(.title3).fontWeight(.bold)
                .foregroundStyle(color)
                .lineLimit(1).minimumScaleFactor(0.6)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.background)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.05), radius: 8, y: 3)
    }
}

// ─── GPS Card ──────────────────────────────────────────────────────
struct GPSCard: View {
    let lat: Double
    let lng: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {

            Label("GPS Location", systemImage: "location.fill")
                .font(.headline).foregroundStyle(.blue)

            HStack {
                // Lat
                VStack(alignment: .leading, spacing: 3) {
                    Text("Latitude").font(.caption).foregroundStyle(.secondary)
                    Text(String(format: "%.6f°", lat))
                        .font(.system(.body, design: .monospaced))
                        .fontWeight(.semibold)
                }
                Spacer()
                // Lng
                VStack(alignment: .leading, spacing: 3) {
                    Text("Longitude").font(.caption).foregroundStyle(.secondary)
                    Text(String(format: "%.6f°", lng))
                        .font(.system(.body, design: .monospaced))
                        .fontWeight(.semibold)
                }
                Spacer()
                // Open Maps
                Button {
                    let url = URL(string: "maps://?ll=\(lat),\(lng)&q=Vehicle")!
                    UIApplication.shared.open(url)
                } label: {
                    Label("Maps", systemImage: "map.fill")
                        .font(.caption).fontWeight(.semibold)
                }
                .buttonStyle(.bordered)
                .tint(.blue)
                .controlSize(.small)
            }
        }
        .padding()
        .background(.background)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.05), radius: 8, y: 3)
    }
}

// ─── Session Stats ─────────────────────────────────────────────────
struct SessionStatsCard: View {
    @ObservedObject var vm: VehicleViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("📊 Session Stats").font(.headline)

            HStack {
                StatPill(label: "Messages",  value: "\(vm.totalReceived)",    icon: "message.fill")
                StatPill(label: "Avg Speed", value: "\(vm.avgSpeed) km/h",    icon: "speedometer")
                StatPill(label: "Max Speed", value: "\(vm.maxSpeed) km/h",    icon: "hare.fill")
            }
        }
        .padding()
        .background(.background)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.05), radius: 8, y: 3)
    }
}

struct StatPill: View {
    let label : String
    let value : String
    let icon  : String

    var body: some View {
        VStack(spacing: 5) {
            Image(systemName: icon)
                .font(.callout).foregroundStyle(.blue)
            Text(value)
                .font(.subheadline).fontWeight(.bold)
            Text(label)
                .font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// ─── Waiting Card ──────────────────────────────────────────────────
struct WaitingCard: View {
    let isConnected: Bool
    @State private var bounce = false

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: isConnected
                  ? "antenna.radiowaves.left.and.right"
                  : "wifi.slash")
            .font(.system(size: 56))
            .foregroundStyle(isConnected ? .blue : .gray)
            .scaleEffect(bounce ? 1.08 : 1.0)
            .onAppear {
                guard isConnected else { return }
                withAnimation(.easeInOut(duration: 0.9).repeatForever()) {
                    bounce = true
                }
            }

            Text(isConnected
                 ? "Waiting for vehicle data..."
                 : "Tap Connect to start")
            .font(.headline).foregroundStyle(.secondary)

            Text(isConnected
                 ? "Make sure Kotlin publisher\nis running & sending data"
                 : "Connects to MQTT broker\nat 127.0.0.1:1883")
            .font(.caption).foregroundStyle(.tertiary)
            .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(40)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

#Preview {
    DashboardView(vm: VehicleViewModel())
}
