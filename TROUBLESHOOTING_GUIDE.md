# 🔧 Troubleshooting Guide: DashboardView Not Receiving Data

## 📋 Problem Summary
DashboardView shows "Waiting for vehicle data..." even though it's connected to the MQTT broker.

---

## ✅ Step 1: Verify Server is Running

### Check if the Kotlin server is still running:
```bash
ps aux | grep java
```
Look for a process running your server JAR or gradle task.

### If NOT running, start it:
```bash
cd /Users/kumkaru/Downloads/KMM-MQTT-Android-IOS-Server
./gradlew :server:run
```

**Expected Output:**
```
✅ MQTT VEHICLE SYSTEM — KOTLIN ONLY
✅ Broker started
✅ iOS Subscriber connected
✅ Ktor Bridge connected  
✅ Android Publisher connected
🔄 CONTINUOUS SIMULATION (Press Ctrl+C to stop)
  ── Cycle 1 ──
📤 [ANDROID] OBD Command → 010C (Engine RPM Request)
...
```

---

## ✅ Step 2: Check iOS Simulator Network

The iOS app needs to reach `127.0.0.1:1883` on your Mac.

### Verify simulator can reach your Mac:
```bash
# On iOS simulator terminal simulator:
ping 127.0.0.1
```

### Or check your Mac's actual IP:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

**Note:** If using a real iPhone (not simulator):
- Update `MQTTManager.swift` line 29:
  ```swift
  private let host : String = "YOUR_MAC_IP"  // e.g., "192.168.1.100"
  ```

---

## ✅ Step 3: Check iOS App Debug Console

1. Open the iOS app
2. **Go to "Debug" tab** (🐛 icon)
3. Look for:
   - **Red connection status** → Connection issue
   - **Green connection status** → Connected ✅
   - **Raw messages appearing** → Data is arriving ✅
   - **No messages** → Data is NOT arriving ❌

### What you should see in Debug console:
```
▶ vehicle/response  [11:45:23]
{
  "speed": 62,
  "rpm": 2100,
  "lat": 19.0760,
  "lng": 72.8777,
  "timestamp": "2026-04-16T11:45:23",
  "deviceId": "device_001"
}
```

---

## ✅ Step 4: Understand the Data Flow

```
Android Publisher                 Ktor Bridge                iOS Subscriber
     ↓                                 ↓                           ↓
sends RPM (010C)    →    receives & caches    →    waiting...
     ↓                                 ↓                           ↓
sends Speed (010D)  →    receives & caches    →    waiting...
     ↓                                 ↓                           ↓
sends GPS location  →    receives all 3 data →    publishes to   ✅ RECEIVES!
                         vehicle/response
```

**⚠️ Important:** The bridge ONLY publishes when it has ALL THREE:
- RPM data (010C)
- Speed data (010D)  
- GPS location (lat, lng)

---

## ✅ Step 5: Common Issues & Fixes

### Issue 1: "No MQTT messages in Debug tab"
**Cause:** Connection failed
**Fix:**
1. Ensure server is running
2. Check host IP (127.0.0.1 for simulator, real IP for phone)
3. Try disconnecting and reconnecting in app

### Issue 2: "Raw messages appear BUT Dashboard shows 'Waiting...'"
**Cause:** JSON parsing failed (structure mismatch)
**Fix:**
1. Check Debug console - raw JSON should match VehicleData model
2. Required fields: `speed`, `rpm`, `lat`, `lng`, `timestamp`, `deviceId`
3. Check console logs in Xcode: `⚠️ [MQTT] Parse error:...`

### Issue 3: "Dashboard shows data for 3 seconds then stops"
**Cause:** Server exited after 3 test cycles (old version)
**Fix:**
1. Update to the new `main.kt` (continuous simulation)
2. Rebuild and run server with: `./gradlew :server:run`

### Issue 4: "Connection shows FAILED on iOS"
**Cause:** Broker unreachable
**Fix:**
1. Verify server is running on correct IP
2. Check firewall isn't blocking port 1883
3. For real iPhone: use correct WiFi IP instead of 127.0.0.1

---

## 🐛 Debug Logging

### iOS Console Logs to Watch:
```
✅ [MQTT] Connected! ID: ios-abc123
📡 [MQTT] Subscribed → vehicle/response
📥 [MQTT] [vehicle/response] {...payload...}
✅ [MQTT] Successfully parsed VehicleData: speed=62 rpm=2100
```

### Server Console Logs to Watch:
```
📨 [KTOR BRIDGE] Received [vehicle/commands]
   🔧 [KTOR BRIDGE] PID 010C (RPM) → 2100
📨 [KTOR BRIDGE] Received [vehicle/gps]
   📍 [KTOR BRIDGE] GPS cached: lat=19.0760, lng=72.8777
   📤 [KTOR BRIDGE] Published → vehicle/response
   JSON: {...}
```

---

## 🔄 Quick Test Procedure

1. **Terminal 1:** Start Kotlin server
   ```bash
   cd /Users/kumkaru/Downloads/KMM-MQTT-Android-IOS-Server
   ./gradlew :server:run
   ```

2. **Terminal 2:** Verify server is publishing
   ```bash
   # Watch for messages being published
   ```

3. **Xcode:** Run iOS app
   ```
   Product → Run (⌘R)
   ```

4. **In iOS App:**
   - Dashboard tab → Click "Connect"
   - Wait 2-3 seconds
   - Should see speed gauge fill with data
   - Debug tab → Should show raw MQTT messages

---

## 📞 Still Not Working?

1. Check Xcode Console for errors
2. Check Terminal for server errors
3. Verify `127.0.0.1:1883` is correct (or use your Mac IP)
4. Check VehicleData.swift structure matches incoming JSON
5. Ensure all fields are present in JSON: speed, rpm, lat, lng, timestamp, deviceId

---

## ✨ Expected Behavior

### When Everything Works:
- ✅ Debug tab shows green "Connected"
- ✅ Raw MQTT messages appear in Debug tab
- ✅ Dashboard shows Speed gauge filling
- ✅ RPM & Device info cards show values
- ✅ GPS location updates
- ✅ Session stats count messages

### When Data Stops Coming:
- Check if server process still running: `ps aux | grep java`
- Check if iOS app is still in foreground
- Try pressing "Disconnect" then "Connect" again

