package org.smarthome.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smarthome.viewmodel.VehicleViewModel

@Composable
fun CommandScreen(vm: VehicleViewModel, innerPadding: PaddingValues) {

    val connectionState by vm.connectionState.collectAsState()
    val isSimulating    by vm.isSimulating.collectAsState()
    val lat             by vm.lat.collectAsState()
    val lng             by vm.lng.collectAsState()
    val statusMessage   by vm.statusMessage.collectAsState()

    val isEnabled = connectionState.isConnected && !isSimulating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("📡 Send Commands", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // ── Status Bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (connectionState.isConnected) Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else Color.Gray.copy(alpha = 0.1f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (connectionState.isConnected)
                    Icons.Default.CheckCircle
                else Icons.Default.Info,
                contentDescription = null,
                tint     = if (connectionState.isConnected) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Text(statusMessage, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }

        // ══════════════════════════════════════════════════════════
        //  SECTION: OBD2 COMMANDS
        // ══════════════════════════════════════════════════════════
        SectionTitle("OBD2 Commands", Icons.Default.DirectionsCar)

        // ── 010C: Engine RPM ──────────────────────────────────────
        OBDCard(
            title       = "Engine RPM",
            pid         = "010C",
            description = "Request engine RPM from ECU\nKtor simulates: 800–4000 RPM",
            icon        = Icons.Default.Speed,
            accentColor = Color(0xFF2196F3),
            topic       = "vehicle/commands",
            enabled     = isEnabled,
            onClick     = { vm.sendRPMCommand() }
        )

        // ── 010D: Vehicle Speed ───────────────────────────────────
        OBDCard(
            title       = "Vehicle Speed",
            pid         = "010D",
            description = "Request vehicle speed from ECU\nKtor simulates: 0–120 km/h",
            icon        = Icons.Default.DirectionsCar,
            accentColor = Color(0xFF4CAF50),
            topic       = "vehicle/commands",
            enabled     = isEnabled,
            onClick     = { vm.sendSpeedCommand() }
        )

        // ══════════════════════════════════════════════════════════
        //  SECTION: GPS
        // ══════════════════════════════════════════════════════════
        SectionTitle("GPS Location", Icons.Default.LocationOn)

        GPSCommandCard(
            lat         = lat,
            lng         = lng,
            enabled     = isEnabled,
            onLatChange = { vm.updateLat(it) },
            onLngChange = { vm.updateLng(it) },
            onSend      = { vm.sendGPS() }
        )

        // ══════════════════════════════════════════════════════════
        //  SECTION: AUTO SIMULATION
        // ══════════════════════════════════════════════════════════
        SectionTitle("Auto Simulation", Icons.Default.PlayCircleFilled)

        SimulationCard(
            isSimulating = isSimulating,
            isConnected  = connectionState.isConnected,
            onStart      = { vm.startSimulation() },
            onStop       = { vm.stopSimulation() }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─── Section Title ─────────────────────────────────────────────────
@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// ─── OBD Command Card ──────────────────────────────────────────────
@Composable
fun OBDCard(
    title       : String,
    pid         : String,
    description : String,
    icon        : ImageVector,
    accentColor : Color,
    topic       : String,
    enabled     : Boolean,
    onClick     : () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, accentColor.copy(alpha = if (enabled) 0.3f else 0.1f))
    ) {
        Row(
            modifier          = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier         = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = accentColor, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Labels
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    // PID Badge
                    Text(
                        text       = pid,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = accentColor,
                        modifier   = Modifier
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(description, fontSize = 11.sp, lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("→ $topic", fontSize = 10.sp, color = accentColor.copy(alpha = 0.7f))
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Send Button
            Button(
                onClick        = onClick,
                enabled        = enabled,
                colors         = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier       = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send", fontSize = 13.sp)
            }
        }
    }
}

// ─── GPS Command Card ──────────────────────────────────────────────
@Composable
fun GPSCommandCard(
    lat         : Double,
    lng         : Double,
    enabled     : Boolean,
    onLatChange : (String) -> Unit,
    onLngChange : (String) -> Unit,
    onSend      : () -> Unit
) {
    val purple = Color(0xFF9C27B0)
    var latText by remember(lat) { mutableStateOf(String.format("%.5f", lat)) }
    var lngText by remember(lng) { mutableStateOf(String.format("%.5f", lng)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, purple.copy(alpha = if (enabled) 0.3f else 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Card Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .background(purple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        tint = purple, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("GPS Location", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("→ vehicle/gps", fontSize = 11.sp, color = purple.copy(alpha = 0.7f))
                    Text("Mumbai: 19.0760°N, 72.8777°E",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lat / Lng text fields
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value           = latText,
                    onValueChange   = { latText = it; onLatChange(it) },
                    label           = { Text("Latitude") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    enabled         = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value           = lngText,
                    onValueChange   = { lngText = it; onLngChange(it) },
                    label           = { Text("Longitude") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    enabled         = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick  = onSend,
                enabled  = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = purple)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send GPS  ($latText, $lngText)")
            }
        }
    }
}

// ─── Auto Simulation Card ──────────────────────────────────────────
@Composable
fun SimulationCard(
    isSimulating : Boolean,
    isConnected  : Boolean,
    onStart      : () -> Unit,
    onStop       : () -> Unit
) {
    val orange = Color(0xFFFF5722)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isSimulating) orange.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surface
        ),
        border   = BorderStroke(
            1.dp,
            if (isSimulating) orange.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .background(
                            (if (isSimulating) orange else Color.Gray).copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint     = if (isSimulating) orange else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        if (isSimulating) "Simulation Running..." else "Auto Simulation",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                    Text("Sends 010C + 010D + GPS every 3 sec",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            // Progress bar while running
            if (isSimulating) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = orange
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start / Stop
            Button(
                onClick  = if (isSimulating) onStop else onStart,
                enabled  = isConnected,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isSimulating) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    if (isSimulating) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null, modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSimulating) "⏹  Stop Simulation" else "▶  Start Auto Simulation")
            }

            if (!isConnected) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Connect to broker first",
                    fontSize = 11.sp,
                    color = Color(0xFFE53935),
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}