package com.winsmediatama.ridercam

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.location.Location
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
    var hasLocationPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var gpsLocation by remember { mutableStateOf<Location?>(null) }
    var gpsAvailable by remember { mutableStateOf(false) }
    var tripDistanceMeters by remember { mutableStateOf(0f) }
    var maxSpeedKmh by remember { mutableStateOf(0f) }
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
        hasPermissions = (result[Manifest.permission.CAMERA] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
            (result[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        status = if (hasPermissions) "Menyiapkan kamera…" else "Izin kamera/mikrofon belum diberikan"
    }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions || !hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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


    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onDispose { }
        } else {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .build()
            var previous: Location? = null
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val current = result.lastLocation ?: return
                    val speed = if (current.hasSpeed()) current.speed * 3.6f else 0f
                    gpsLocation = current
                    maxSpeedKmh = maxOf(maxSpeedKmh, speed)
                    previous?.let { old ->
                        val delta = old.distanceTo(current)
                        if (delta in 0.5f..100f && current.accuracy <= 35f) tripDistanceMeters += delta
                    }
                    previous = current
                }
            }
            try {
                client.requestLocationUpdates(request, callback, context.mainLooper)
            } catch (_: SecurityException) {
                hasLocationPermission = false
            }
            onDispose { client.removeLocationUpdates(callback) }
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
                    Text("Cam", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("Rider", color = Neon, fontSize = 26.sp, fontWeight = FontWeight.Black)
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
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
            SpeedometerDial(
                speedKmh = gpsLocation?.let { if (it.hasSpeed()) it.speed * 3.6f else 0f } ?: 0f,
                modifier = Modifier.align(Alignment.Center).fillMaxHeight(0.574f).aspectRatio(1f)
            )
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

        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xE6000000), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x553E4A4F), RoundedCornerShape(14.dp)).padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelemetryCell("JARAK", "%.2f km".format(Locale.US, tripDistanceMeters / 1000f), Icons.Default.GpsFixed, Modifier.weight(1f))
            TelemetryCell("MAX SPEED", "%.0f km/h".format(Locale.US, maxSpeedKmh), Icons.Default.Speed, Modifier.weight(1f))
            TelemetryCell("ALTITUDE", gpsLocation?.takeIf { it.hasAltitude() }?.let { "%.0f m".format(Locale.US, it.altitude) } ?: "— m", Icons.Default.GpsFixed, Modifier.weight(1f))
            TelemetryCell("GPS", gpsLocation?.let { "±%.0f m".format(Locale.US, it.accuracy) } ?: "Mencari", Icons.Default.GpsFixed, Modifier.weight(1f))
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
                                put(MediaStore.Video.Media.DISPLAY_NAME, "CamRider_$stamp.mp4")
                                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CamRider")
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
                                            "Video tersimpan di Movies/CamRider"
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
            "Jarak %.2f km  •  Maks %.0f km/h  •  Koordinat %s".format(Locale.US, tripDistanceMeters / 1000f, maxSpeedKmh, gpsLocation?.let { "%.5f, %.5f".format(Locale.US, it.latitude, it.longitude) } ?: "menunggu GPS"),
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

@Composable
private fun SpeedometerDial(speedKmh: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(size.width, size.height) * 0.48f
            val center = androidx.compose.ui.geometry.Offset(cx, cy)
            val dialRect = androidx.compose.ui.geometry.Offset(cx - radius * 0.86f, cy - radius * 0.86f)
            val dialSize = androidx.compose.ui.geometry.Size(radius * 1.72f, radius * 1.72f)
            drawCircle(Color(0xDD050708), radius, center)
            drawCircle(Color(0xFF8C989D), radius, center, style = Stroke(radius * 0.025f))
            drawArc(Neon, 135f, 172f, false, dialRect, dialSize, style = Stroke(radius * 0.065f))
            drawArc(Color(0xFFFFA52C), 307f, 25f, false, dialRect, dialSize, style = Stroke(radius * 0.065f))
            drawArc(Red, 332f, 63f, false, dialRect, dialSize, style = Stroke(radius * 0.065f))
            for (i in 0..40) {
                val angle = Math.toRadians(135.0 + i * 270.0 / 40.0)
                val inner = radius * if (i % 4 == 0) 0.68f else 0.75f
                val outer = radius * 0.82f
                val p1 = androidx.compose.ui.geometry.Offset(cx + cos(angle).toFloat() * inner, cy + sin(angle).toFloat() * inner)
                val p2 = androidx.compose.ui.geometry.Offset(cx + cos(angle).toFloat() * outer, cy + sin(angle).toFloat() * outer)
                drawLine(if (i > 33) Red else Color.White, p1, p2, if (i % 4 == 0) radius * 0.018f else radius * 0.008f)
                if (i % 4 == 0) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = radius * 0.10f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    }
                    val tx = cx + cos(angle).toFloat() * radius * 0.56f
                    val ty = cy + sin(angle).toFloat() * radius * 0.56f + radius * 0.035f
                    drawContext.canvas.nativeCanvas.drawText((i * 5).toString(), tx, ty, paint)
                }
            }
            val angle = Math.toRadians(135.0 + speedKmh.coerceIn(0f, 200f) / 200f * 270.0)
            val tip = androidx.compose.ui.geometry.Offset(cx + cos(angle).toFloat() * radius * 0.58f, cy + sin(angle).toFloat() * radius * 0.58f)
            val needle = Path().apply { moveTo(cx, cy); lineTo(tip.x, tip.y) }
            drawPath(needle, Red, style = Stroke(radius * 0.03f))
            drawCircle(Color(0xFFB8C1C4), radius * 0.075f, center)
            drawCircle(Color(0xFF07090A), radius * 0.045f, center)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                .background(Color(0xF0000000), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x553E4A4F), RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("%.0f".format(Locale.US, speedKmh.coerceAtLeast(0f)), color = Color.White,
                fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
            Text("km/h", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TelemetryCell(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Neon, modifier = Modifier.size(15.dp))
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        Text(label, color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}
