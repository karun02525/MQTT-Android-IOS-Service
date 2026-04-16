# 🚀 Quick Start Guide — After Applying Fixes

## ⚡ TL;DR — Get Running in 60 Seconds

### Terminal 1: Start the Server (Keep Running)
```bash
cd /Users/kumkaru/Downloads/KMM-MQTT-Android-IOS-Server
./gradlew :server:run
```

Wait for this output:
```
✅ MQTT VEHICLE SYSTEM
🔄 CONTINUOUS SIMULATION (Press Ctrl+C to stop)
  ── Cycle 1 ──
```

### Terminal 2: Run iOS App
```bash
# In Xcode:
Product → Run (�cmd+R)
```

### Step 3: In iOS App
1. Open the **Dashboard** tab (gauge icon)
2. Click the **"Connect"** button
3. 🎉 **Speed gauge should appear in 1-2 seconds**

---

## 🐛 Debugging Checklist

| What to Check | Where | Expected |
|---|---|---|
| Is server running? | Terminal 1 | "CONTINUOUS SIMULATION" message |
| Is iOS connected? | App → Debug tab | Green dot + "Connected" |
| Are messages arriving? | App → Debug tab | Raw JSON appearing |
| Is data being parsed? | Xcode console | "✅ Successfully parsed VehicleData" |
| Is Dashboard showing data? | App → Dashboard tab | Speed gauge + values |

---

## 🔴 If Dashboard Still Shows "Waiting..."

### Quick Fixes (in order):
1. **Restart server:** Kill Terminal 1, run `./gradlew :server:run` again
2. **Reconnect app:** Click "Disconnect" then "Connect"  
3. **Check Debug tab:** 
   - No messages? → Network/connection issue
   - Messages appear? → Parsing issue (check Xcode logs)
4. **Check IP:** If using real iPhone, update `MQTTManager.swift` line 29 with your Mac IP
5. **Check logs:** Look for "❌ [MQTT] Parse error:" in Xcode console

---

## 📊 Server vs iOS Data Path

```
SERVER sends:                    iOS displays:
RPM data (010C)          →       [Delayed until all 3 arrive]
Speed data (010D)        →       [Still waiting...]
GPS location             →       [Now has all 3!] → 📊 Dashboard updates!
```

**Key:** Server waits for ALL 3 data points before publishing.

---

## 📈 Expected Message Flow

```
Server Output:                   iOS Debug Tab:
──────────────────              ────────────────
Cycle 1                          Messages: 1
  ── RPM sent                     Parsed: 0
  ── Speed sent                   [raw message appears]
  ── GPS sent
  ── Published to vehicle/response
                                 ✅ Successfully parsed
                                 Messages: 1
                                 Parsed: 1
Cycle 2
  ── RPM sent                     [Dashboard updates with speed!]
  ── Speed sent                   Latest: 🚗 62 km/h
  ── GPS sent                     RPM: 2100
  ── Published
                                 Messages: 2
                                 Parsed: 2
```

---

## 💡 Pro Tips

### Tip 1: Monitor Both Terminals
- **Terminal 1:** Watch server output to confirm data is being sent
- **Terminal 2:** Watch Xcode console for iOS logs

### Tip 2: Use Debug Tab for Troubleshooting
- Green = Connected ✅
- Orange = Connecting...
- Gray = Disconnected
- Red = Failed (check server running)

### Tip 3: Check Console Before Restarting
```
# In Xcode console, search for:
[MQTT]  ← Shows connection events
Parse error: ← Shows JSON parsing issues
Successfully parsed ← Shows successful data arrival
```

### Tip 4: For Real iPhone
Edit `MQTTManager.swift` line 29:
```swift
// Change from:
private let host : String = "127.0.0.1"

// To your Mac's WiFi IP (get it from: System Settings → WiFi → Details):
private let host : String = "192.168.1.100"  // Your Mac IP here
```

---

## 🎬 Expected User Experience

### First Time:
1. App shows "Disconnected" (gray dot)
2. Click "Connect" button
3. Status changes to "Connecting..." (orange)
4. After ~1-2 seconds: "Connected" (green)
5. Speed gauge appears and fills to current speed
6. RPM, GPS, and stats update every 2 seconds
7. History tab shows all received data points

### Continuous Use:
- Dashboard updates smoothly every 2 seconds
- Speed gauge animates between values
- Stats accumulate (message count, max speed)
- Click "Disconnect" and "Connect" to test reconnection

---

## 🛠️ Emergency Fixes

### If Nothing Shows in Debug Tab:
```bash
# Kill any lingering server processes
pkill -f "org.smarthome"

# Restart fresh
./gradlew :server:run
```

### If "Parse error" Appears:
Check JSON structure matches VehicleData fields:
```swift
required: speed, rpm, lat, lng, timestamp, deviceId
example: {"speed":62,"rpm":2100,"lat":19.076,"lng":72.877,"timestamp":"2026-04-16T11:45:23","deviceId":"device_001"}
```

### If App Crashes:
Check Xcode console for crash log, likely due to:
- Missing CocoaMQTT dependency (run `pod install`)
- Simulator not connected to broker (restart simulator)

---

## ✨ What's Been Fixed

✅ Server now runs continuously (not 4-second timeout)
✅ Enhanced iOS logging shows parse errors
✅ Debug tab now shows real-time stats
✅ Better error messages in console
✅ Status header shows latest data

---

## 📞 Support Info

If still stuck after these steps, check:
1. `TROUBLESHOOTING_GUIDE.md` for detailed troubleshooting
2. `FIX_SUMMARY.md` for technical details of changes
3. Xcode console logs for specific errors
4. Terminal server logs for connection issues

---

**Happy tracking! 🚗📡**

