//
//  ContentViewMqtt.swift
//  iosApp
//
//  Created by Kumar, Karun (893) (EXT) on 16/04/26.
//

// Views/ContentView.swift
import SwiftUI

struct ContentViewMqtt: View {

    // One ViewModel shared across all tabs
    @StateObject private var vm = VehicleViewModel()
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {

            // ── Tab 1: Live Dashboard ─────────────────────────────
            DashboardView(vm: vm)
                .tabItem {
                    Label("Dashboard", systemImage: "gauge.high")
                }
                .tag(0)

            // ── Tab 2: History ────────────────────────────────────
            HistoryView(vm: vm)
                .tabItem {
                    Label("History", systemImage: "list.bullet")
                }
                .tag(1)

            // ── Tab 3: Debug ──────────────────────────────────────
            DebugView(vm: vm)
                .tabItem {
                    Label("Debug", systemImage: "terminal")
                }
                .tag(2)
        }
    }
}

#Preview {
    ContentView()
}
