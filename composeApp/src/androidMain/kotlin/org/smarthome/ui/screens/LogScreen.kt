package org.smarthome.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import org.smarthome.models.SentMessage
import org.smarthome.viewmodel.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(vm: VehicleViewModel, innerPadding: PaddingValues) {

    val sentMessages by vm.sentMessages.collectAsState()
    val totalSent    by vm.totalSent.collectAsState()

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar   = {
            TopAppBar(
                title = {
                    Column {
                        Text("📋 Message Log", fontWeight = FontWeight.Bold)
                        Text(
                            "$totalSent messages sent",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.clearLog() }) {
                        Icon(Icons.Default.DeleteSweep,
                            contentDescription = "Clear log",
                            tint = Color(0xFFE53935))
                    }
                }
            )
        },
        containerColor = Color(0xFF0D0D0D)   // Dark background like terminal
    ) { padding ->

        if (sentMessages.isEmpty()) {
            // ── Empty State ────────────────────────────────────────
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null,
                        tint = Color.DarkGray, modifier = Modifier.size(56.dp))
                    Text("No messages yet",
                        color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                    Text("Go to Commands tab and send OBD or GPS data",
                        fontSize = 13.sp, color = Color.DarkGray)
                }
            }
        } else {
            // ── Log List ───────────────────────────────────────────
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentPadding      = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sentMessages, key = { it.id }) { msg ->
                    LogRow(msg = msg)
                }
            }
        }
    }
}

// ─── Single Log Row ────────────────────────────────────────────────
@Composable
fun LogRow(msg: SentMessage) {

    // Color-code by topic
    val topicColor = when {
        msg.topic.contains("commands") -> Color(0xFF2196F3)   // Blue  = OBD
        msg.topic.contains("gps")      -> Color(0xFF9C27B0)   // Purple = GPS
        msg.topic.contains("response") -> Color(0xFF4CAF50)   // Green  = Response
        else                           -> Color(0xFF9E9E9E)
    }

    val topicEmoji = when {
        msg.topic.contains("commands") -> "⚙️"
        msg.topic.contains("gps")      -> "📍"
        msg.topic.contains("response") -> "📥"
        else                           -> "📨"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // ── Header Row ────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Terminal prompt
            Text("▶", color = Color(0xFF4CAF50), fontSize = 12.sp,
                fontFamily = FontFamily.Monospace)

            // Topic pill
            Text(
                text       = "$topicEmoji ${msg.topic}",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.Black,
                modifier   = Modifier
                    .background(topicColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Timestamp
            Text(msg.timeLabel, fontSize = 10.sp,
                color = Color(0xFF666666), fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── JSON Payload ──────────────────────────────────────────
        Text(
            text       = prettyJson(msg.payload),
            fontFamily = FontFamily.Monospace,
            fontSize   = 12.sp,
            color      = Color(0xFF4CAF50),
            lineHeight = 18.sp,
            modifier   = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        )
    }
}

// ─── Pretty Print JSON ─────────────────────────────────────────────
// Uses Android's built-in JSONObject.toString(indent)
private fun prettyJson(raw: String): String {
    return try {
        JSONObject(raw).toString(2)
    } catch (e: Exception) {
        raw  // Return as-is if not valid JSON
    }
}