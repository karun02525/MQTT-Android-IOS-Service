// Views/HistoryView.swift
import SwiftUI

struct HistoryView: View {
    @ObservedObject var vm: VehicleViewModel

    var body: some View {
        NavigationStack {
            Group {
                if vm.history.isEmpty {
                    // ── Empty ─────────────────────────────────────
                    ContentUnavailableView(
                        "No History Yet",
                        systemImage: "tray",
                        description: Text("Data will appear here as\nMQTT messages arrive")
                    )
                } else {
                    // ── List ──────────────────────────────────────
                    List(vm.history) { item in
                        HistoryRow(data: item)
                            .listRowInsets(
                                EdgeInsets(top: 6, leading: 16,
                                           bottom: 6, trailing: 16)
                            )
                            .listRowSeparator(.hidden)
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("History (\(vm.history.count))")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Clear", role: .destructive) {
                        vm.clearAll()
                    }
                    .disabled(vm.history.isEmpty)
                }
            }
        }
    }
}

// ─── Single Row ────────────────────────────────────────────────────
struct HistoryRow: View {
    let data: VehicleData

    var body: some View {
        HStack(spacing: 12) {

            // Speed badge
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(data.speedColor.opacity(0.12))
                    .frame(width: 64, height: 64)
                VStack(spacing: 0) {
                    Text("\(data.speed)")
                        .font(.title2).fontWeight(.bold)
                        .foregroundStyle(data.speedColor)
                    Text("km/h")
                        .font(.caption2).foregroundStyle(.secondary)
                }
            }

            // Details
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Label("\(data.rpm) RPM", systemImage: "gear")
                        .font(.subheadline).fontWeight(.medium)
                        .foregroundStyle(data.rpmColor)
                    Spacer()
                    Text(data.timeOnly)
                        .font(.caption).foregroundStyle(.secondary)
                }
                HStack(spacing: 4) {
                    Image(systemName: "location.fill")
                        .font(.caption2).foregroundStyle(.blue)
                    Text("\(data.latFormatted), \(data.lngFormatted)")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Text(data.deviceId)
                    .font(.caption2).foregroundStyle(.purple)
                    .padding(.horizontal, 6).padding(.vertical, 2)
                    .background(.purple.opacity(0.08))
                    .clipShape(Capsule())
            }
        }
        .padding(10)
        .background(.background)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(color: .black.opacity(0.04), radius: 6, y: 2)
    }
}

#Preview {
    HistoryView(vm: VehicleViewModel())
}
