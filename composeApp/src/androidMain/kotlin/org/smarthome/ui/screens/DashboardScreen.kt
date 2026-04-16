package org.smarthome.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smarthome.models.ConnectionState
import org.smarthome.models.VehicleResponse
import org.smarthome.viewmodel.VehicleViewModel


@Composable
fun DashboardScreen(vm: VehicleViewModel, innerPadding: PaddingValues) {

    val connectionState by vm.connectionState.collectAsState()
    val statusMessage   by vm.statusMessage.collectAsState()
    val latestResponse  by vm.latestResponse.collectAsState()
    val totalSent       by vm.totalSent.collectAsState()
    val brokerHost      by vm.brokerHost.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Title ──────────────────────────────────────────────────
        Text(
            text       = "🚗 Vehicle MQTT Sender",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Connection Banner ──────────────────────────────────────
        ConnectionBanner(
            state        = connectionState,
            statusMsg    = statusMessage,
            brokerHost   = brokerHost,
            onHostChange = { vm.updateBrokerHost(it) },
            onToggle     = { vm.toggleConnection() }
        )

        // ── Stats Row ──────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.Send,
                title    = "Sent",
                value    = "$totalSent",
                color    = MaterialTheme.colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.Wifi,
                title    = "Status",
                value    = connectionState.label,
                color    = connectionState.color
            )
        }

        // ── Device Info ────────────────────────────────────────────
        DeviceInfoCard(deviceId = vm.deviceId, host = brokerHost)

        // ── Latest Response from Ktor ──────────────────────────────
        Text(
            text       = "📥 Server Response (vehicle/response)",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        if (latestResponse != null) {
            ResponseCard(response = latestResponse!!)
        } else {
            WaitingCard(isConnected = connectionState.isConnected)
        }
    }
}

// ─── Connection Banner ─────────────────────────────────────────────
@Composable
fun ConnectionBanner(
    state       : ConnectionState,
    statusMsg   : String,
    brokerHost  : String,
    onHostChange: (String) -> Unit,
    onToggle    : () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editText by remember(brokerHost) { mutableStateOf(brokerHost) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = state.color.copy(alpha = 0.08f)
        ),
        border   = BorderStroke(1.dp, state.color.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Main Row ──────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing dot
                PulsingDot(color = state.color, isActive = state == ConnectionState.CONNECTED)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(statusMsg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "$brokerHost:1883  •  topic: vehicle/commands",
                        fontSize = 10.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }

                // Edit host icon
                IconButton(
                    onClick  = { showEdit = !showEdit },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Host",
                         modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Connect / Disconnect button
                Button(
                    onClick        = onToggle,
                    colors         = ButtonDefaults.buttonColors(
                        containerColor = if (state.isConnected) Color(0xFFE53935)
                                         else MaterialTheme.colorScheme.primary
                    ),
                    modifier       = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text(
                        text     = if (state.isConnected) "Disconnect" else "Connect",
                        fontSize = 13.sp
                    )
                }
            }

            // ── Broker Host Editor ────────────────────────────────
            if (showEdit) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value           = editText,
                        onValueChange   = { editText = it },
                        label           = { Text("Broker Host") },
                        placeholder     = { Text("10.0.2.2") },
                        modifier        = Modifier.weight(1f),
                        singleLine      = true,
                        supportingText  = {
                            Text(
                                "Emulator: 10.0.2.2  |  Real device: 192.168.x.x",
                                fontSize = 10.sp
                            )
                        }
                    )
                    Button(onClick = {
                        onHostChange(editText)
                        showEdit = false
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ─── Pulsing Dot ──────────────────────────────────────────────────
@Composable
fun PulsingDot(color: Color, isActive: Boolean) {
    val scale by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue  = 1f,
            targetValue   = if (isActive) 1.5f else 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
    ) {
        Box(
            modifier = Modifier
                .size((14 * scale).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
                .align(Alignment.Center)
        )
    }
}

// ─── Stat Card ─────────────────────────────────────────────────────
@Composable
fun StatCard(
    modifier : Modifier = Modifier,
    icon     : ImageVector,
    title    : String,
    value    : String,
    color    : Color
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border   = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null,
                 tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

// ─── Device Info Card ──────────────────────────────────────────────
@Composable
fun DeviceInfoCard(deviceId: String, host: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Device ID",
                     fontSize = 11.sp,
                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(deviceId, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Broker: $host:1883",
                     fontSize = 11.sp,
                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

// ─── Response Card — Shows what iOS would receive ──────────────────
@Composable
fun ResponseCard(response: VehicleResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.06f)),
        border   = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                     tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Response received from Ktor server",
                     fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speed + RPM
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DataChip("🏎  Speed",  "${response.speed} km/h", Color(0xFF2196F3), Modifier.weight(1f))
                DataChip("⚙️  RPM",    "${response.rpm}",         Color(0xFFFF9800), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GPS
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DataChip("📍 Lat", String.format("%.5f°", response.lat), Color(0xFF9C27B0), Modifier.weight(1f))
                DataChip("📍 Lng", String.format("%.5f°", response.lng), Color(0xFF9C27B0), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp + Device
            Text(
                text     = "🕒 ${response.timestamp}",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text     = "📱 ${response.deviceId}",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Data Chip ─────────────────────────────────────────────────────
@Composable
fun DataChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 10.sp,
             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
    }
}

// ─── Waiting Card ──────────────────────────────────────────────────
@Composable
fun WaitingCard(isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier            = Modifier.padding(36.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector        = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint               = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier           = Modifier.size(48.dp)
            )
            Text(
                text       = if (isConnected) "Waiting for server response..." else "Not connected",
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text      = if (isConnected)
                                "Send a command from\nthe Commands tab first"
                            else
                                "Tap Connect to start",
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}