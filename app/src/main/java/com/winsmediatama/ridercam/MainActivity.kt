package com.winsmediatama.ridercam

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var rearCamera by remember { mutableStateOf(true) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Izinkan kamera dan mikrofon untuk mulai") }
    var cameraReady by remember { mutableStateOf(false) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions = result[Manifest.permission.CAMERA] == true &&
            result[Manifest.permission.RECORD_AUDIO] == true
        status = if (hasPermissions) "Menyiapkan kamera…" else "Izin kamera/mikrofon belum diberikan"
    }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    LaunchedEffect(hasPermissions, rearCamera) {
        if (!hasPermissions) return@LaunchedEffect
        cameraReady = false
        status = "Menyiapkan kamera…"
        try {
            val provider = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(context))
            }
            provider.unbindAll()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                )
                .build()
            val capture = VideoCapture.withOutput(recorder)
            val selector = if (rearCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            videoCapture = capture
            cameraReady = true
            status = "Kamera siap • target Full HD"
        } catch (error: Exception) {
            videoCapture = null
            cameraReady = false
            status = "Kamera gagal dibuka: ${error.localizedMessage ?: "periksa perangkat"}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Night).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).border(2.dp, Neon, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text("R", color = Neon, fontSize = 29.sp, fontWeight = FontWeight.Black) }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rider", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("Cam", color = Neon, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
                Text("RIDE • RECORD • TRACK • ANALYZE", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = Color.White)
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .background(Color(0xFF182226), RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF35444A), RoundedCornerShape(18.dp))
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (!cameraReady) {
                Box(Modifier.fillMaxSize().background(Color(0x99070B0D)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Neon, modifier = Modifier.size(40.dp))
                        Text(status, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                        if (!hasPermissions) {
                            Button(onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                            }) { Text("Izinkan Kamera") }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    .background(Color(0xDD000000), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(if (recording) Red else Muted, CircleShape))
                Text(if (recording) " REC" else " STANDBY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(" •  FHD", color = Muted, fontSize = 10.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { if (!recording) rearCamera = true },
                enabled = !recording,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (rearCamera) Neon else Panel)
            ) { Text("Kamera Belakang", color = if (rearCamera) Night else Color.White, fontSize = 12.sp) }
            Button(
                onClick = { if (!recording) rearCamera = false },
                enabled = !recording,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (!rearCamera) Neon else Panel)
            ) { Text("Kamera Depan", color = if (!rearCamera) Night else Color.White, fontSize = 12.sp) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f).background(Panel, RoundedCornerShape(16.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Neon, modifier = Modifier.size(18.dp))
                    Text(" GPS SPEED", color = Muted, fontSize = 11.sp)
                }
                Text("--", color = Neon, fontSize = 44.sp, fontWeight = FontWeight.Black, lineHeight = 46.sp)
                Text("km/h • GPS belum aktif", color = Muted, fontSize = 10.sp)
            }
            Column(
                modifier = Modifier.weight(1f).background(Panel, RoundedCornerShape(16.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                TelemetryLine(Icons.Default.GpsFixed, "GPS", "Belum aktif")
                TelemetryLine(Icons.Default.CameraAlt, "Kamera", if (rearCamera) "Belakang" else "Depan")
                TelemetryLine(Icons.Default.Mic, "Status", if (cameraReady) "Siap merekam" else "Menunggu")
            }
        }

        Button(
            onClick = {
                if (recording) {
                    activeRecording?.stop()
                    activeRecording = null
                    recording = false
                    status = "Rekaman dihentikan"
                } else {
                    val capture = videoCapture
                    if (capture == null || !cameraReady) {
                        status = "Kamera belum siap"
                    } else {
                        try {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val values = ContentValues().apply {
                                put(MediaStore.Video.Media.DISPLAY_NAME, "RiderCam_$stamp.mp4")
                                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RiderCam")
                            }
                            val output = MediaStoreOutputOptions.Builder(
                                context.contentResolver,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            ).setContentValues(values).build()
                            var pending = capture.output.prepareRecording(context, output)
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                pending = pending.withAudioEnabled()
                            }
                            activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> {
                                        recording = true
                                        status = "Sedang merekam"
                                    }
                                    is VideoRecordEvent.Finalize -> {
                                        recording = false
                                        activeRecording = null
                                        status = if (event.hasError()) {
                                            "Gagal menyelesaikan video (${event.error})"
                                        } else {
                                            "Video tersimpan di Movies/RiderCam"
                                        }
                                    }
                                }
                            }
                        } catch (error: Exception) {
                            recording = false
                            status = "Tidak dapat merekam: ${error.localizedMessage ?: "error"}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (recording) Red else Neon)
        ) {
            Icon(if (recording) Icons.Default.Stop else Icons.Default.Videocam, contentDescription = null,
                tint = if (recording) Color.White else Night)
            Text(if (recording) "  STOP & SIMPAN" else "  MULAI REKAM", color = if (recording) Color.White else Night,
                fontWeight = FontWeight.Black)
        }

        Text(status, modifier = Modifier.fillMaxWidth(), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
        Text(
            "GPS overlay dan rekaman bersegmen otomatis belum diaktifkan pada tahap ini.",
            modifier = Modifier.fillMaxWidth(), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center
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
