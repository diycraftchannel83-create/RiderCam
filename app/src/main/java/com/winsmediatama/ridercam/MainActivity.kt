package com.winsmediatama.ridercam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF070B0D)
private val Panel = Color(0xFF11191D)
private val Neon = Color(0xFF72FF24)
private val Muted = Color(0xFF9BA8AD)
private val Red = Color(0xFFFF334D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Night) {
                    RiderCamDashboard()
                }
            }
        }
    }
}

@Composable
private fun RiderCamDashboard() {
    var recording by remember { mutableStateOf(false) }
    var rearCamera by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize().background(Night).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).border(2.dp, Neon, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("R", color = Neon, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rider", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("Cam", color = Neon, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Text("RIDE • RECORD • TRACK • ANALYZE", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }

        // Prototype placeholder. Replace with CameraX PreviewView before production recording.
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .background(Color(0xFF182226), RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF35444A), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = Neon, modifier = Modifier.size(42.dp))
                Text("LIVE CAMERA PREVIEW", color = Color.White, fontWeight = FontWeight.Bold)
                Text("CameraX integration pending", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { rearCamera = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (rearCamera) Neon else Panel)
                    ) { Text("Belakang", color = if (rearCamera) Night else Color.White) }
                    Button(
                        onClick = { rearCamera = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (!rearCamera) Neon else Panel)
                    ) { Text("Depan", color = if (!rearCamera) Night else Color.White) }
                }
            }
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(if (recording) Red else Muted, CircleShape))
                Text(if (recording) " REC" else " STANDBY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("  •  1080p / 60 target", color = Muted, fontSize = 10.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f).background(Panel, RoundedCornerShape(18.dp)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Neon, modifier = Modifier.size(18.dp))
                    Text(" GPS SPEED", color = Muted, fontSize = 11.sp)
                }
                Text("--", color = Neon, fontSize = 52.sp, fontWeight = FontWeight.Black, lineHeight = 54.sp)
                Text("km/h • menunggu GPS", color = Muted, fontSize = 11.sp)
            }
            Column(
                modifier = Modifier.weight(1f).background(Panel, RoundedCornerShape(18.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryLine(Icons.Default.GpsFixed, "GPS", "Belum aktif")
                TelemetryLine(Icons.Default.CameraAlt, "Kamera", if (rearCamera) "Belakang" else "Depan")
                TelemetryLine(Icons.Default.Mic, "Audio", "Siap dikonfigurasi")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { recording = !recording },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (recording) Red else Neon)
            ) {
                Icon(if (recording) Icons.Default.Stop else Icons.Default.Videocam, contentDescription = null,
                    tint = if (recording) Color.White else Night)
                Text(if (recording) " STOP" else " MULAI REKAM", color = if (recording) Color.White else Night,
                    fontWeight = FontWeight.Black)
            }
        }

        Text(
            "DISTANCE  -- km     MAX SPEED  -- km/h     ALTITUDE  -- m",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Prototype UI • angka GPS dan rekaman belum terhubung ke sensor/kamera",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TelemetryLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Neon, modifier = Modifier.size(17.dp))
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
