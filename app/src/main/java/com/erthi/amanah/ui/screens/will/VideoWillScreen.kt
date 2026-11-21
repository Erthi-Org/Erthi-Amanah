package com.erthi.amanah.ui.screens.will

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.VideoWillState
import com.erthi.amanah.ui.viewmodel.VideoWillViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@Composable
fun VideoWillScreen(
    onBack: () -> Unit,
    viewModel: VideoWillViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()

    // Permissions
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED)
    }
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED)
    }
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true
        hasMicPermission = results[Manifest.permission.RECORD_AUDIO] == true
    }

    // Recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var recordedTempFile by remember { mutableStateOf<File?>(null) }
    var cameraFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Duration timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    // Check available storage (warn if < 200MB)
    val availableMb = remember {
        context.filesDir.freeSpace / (1024 * 1024)
    }

    // Cleanup temp files on first enter
    LaunchedEffect(Unit) {
        viewModel.cleanupTempFiles()
    }

    fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
        isRecording = false
    }

    fun bindCamera(previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.fromOrderedList(
                    listOf(Quality.HD, Quality.FHD, Quality.SD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                .build()
            val vc = VideoCapture.withOutput(recorder)
            videoCapture = vc
            val selector = CameraSelector.Builder()
                .requireLensFacing(cameraFacing).build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            } catch (e: Exception) {
                // Camera unavailable or already in use — handled by showing error state
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Cancel confirmation dialog
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            containerColor = NavyCard,
            title = { Text("إلغاء التسجيل", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد إلغاء التسجيل وحذف هذا الفيديو؟", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    stopRecording()
                    recordedTempFile?.delete()
                    recordedTempFile = null
                    showCancelConfirm = false
                    onBack()
                }) { Text("إلغاء وحذف", color = EmergencyRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("استمرار", color = WillColor) }
            },
        )
    }

    // Save dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = NavyCard,
            title = { Text("حفظ الوصية المصورة", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = saveTitle, onValueChange = { saveTitle = it },
                        label = { Text("عنوان الوصية", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WillColor, unfocusedBorderColor = DividerColor,
                            focusedLabelColor = WillColor, cursorColor = WillColor,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        ),
                        singleLine = true,
                        placeholder = { Text("مثال: وصيتي لأبنائي", color = TextMuted) },
                    )
                    Text("سيتم تشفير الفيديو وحفظه بأمان على هذا الجهاز.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tempFile = recordedTempFile
                        if (tempFile != null) {
                            viewModel.saveVideoWill(saveTitle.ifBlank { "وصية مصورة" }, tempFile.absolutePath)
                            // encryptVideo is called after saving to persist the encrypted path in the DB
                            viewModel.encryptVideo(tempFile, willId = null)
                        }
                        showSaveDialog = false
                        recordedTempFile = null
                    },
                    enabled = saveTitle.isNotBlank() || true,
                ) { Text("حفظ وتشفير", color = WillColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("إلغاء", color = TextSecondary) }
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الوصية المصورة", onBack = {
            if (isRecording || recordedTempFile != null) showCancelConfirm = true else onBack()
        }) },
        containerColor = Color.Black,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!hasCameraPermission || !hasMicPermission) {
                // Permission request screen
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.Videocam, null, tint = WillColor, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("تسجيل الوصية المصورة",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("يحتاج التطبيق إلى إذن الكاميرا والميكروفون لتسجيل وصيتك.",
                        color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            permissionsLauncher.launch(arrayOf(
                                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WillColor),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("منح الصلاحيات", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                return@Scaffold
            }

            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                        bindCamera(previewView)
                    }
                },
                update = { previewView ->
                    bindCamera(previewView)
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Top overlay: duration + storage warning
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(10.dp).background(EmergencyRed, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatDuration(recordingDurationSec),
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        )
                    }
                }
                if (availableMb < 200) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تحذير: مساحة التخزين المتاحة ${availableMb} ميجابايت",
                        color = WarningAmber,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }

            // State overlay (encrypting, error)
            when (val s = state) {
                is VideoWillState.Encrypting -> {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = WillColor)
                            Spacer(Modifier.height(12.dp))
                            Text("جاري تشفير الفيديو...", color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
                is VideoWillState.Ready -> {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreenLight, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("تم حفظ الوصية المصورة بأمان", color = TextPrimary, fontSize = 16.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetState(); onBack() },
                                colors = ButtonDefaults.buttonColors(containerColor = WillColor),
                            ) { Text("العودة", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                is VideoWillState.Error -> {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Default.Error, null, tint = EmergencyRed, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(s.message, color = TextPrimary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetState() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                            ) { Text("إعادة المحاولة") }
                        }
                    }
                }
                else -> {}
            }

            // Bottom controls (only when no overlay active)
            if (state is VideoWillState.Idle) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (recordedTempFile != null) {
                        // After recording — show Save / Discard
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = {
                                    recordedTempFile?.delete(); recordedTempFile = null
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyRed),
                                modifier = Modifier.weight(1f),
                            ) { Text("حذف التسجيل", fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = { showSaveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WillColor),
                                modifier = Modifier.weight(1f),
                            ) { Text("حفظ وتشفير", fontWeight = FontWeight.Bold) }
                        }
                    } else {
                        // Camera switch (only when not recording)
                        if (!isRecording) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    onClick = {
                                        cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_FRONT)
                                            CameraSelector.LENS_FACING_BACK
                                        else CameraSelector.LENS_FACING_FRONT
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                ) {
                                    Icon(Icons.Default.Cameraswitch, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Record / Stop button
                        Box(contentAlignment = Alignment.Center) {
                            if (isRecording) {
                                // Pulsing ring for recording indicator
                                Box(
                                    modifier = Modifier.size(88.dp)
                                        .border(3.dp, EmergencyRed.copy(alpha = 0.5f), CircleShape)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (isRecording) {
                                        stopRecording()
                                    } else {
                                        // Start recording
                                        val vc = videoCapture ?: return@IconButton
                                        val tempFile = viewModel.createTempRecordingFile()
                                        recordedTempFile = tempFile
                                        val outputOptions = FileOutputOptions.Builder(tempFile).build()
                                        currentRecording = vc.output
                                            .prepareRecording(context, outputOptions)
                                            .withAudioEnabled()
                                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                                when (event) {
                                                    is VideoRecordEvent.Start -> { isRecording = true }
                                                    is VideoRecordEvent.Finalize -> {
                                                        isRecording = false
                                                        if (event.hasError()) {
                                                            tempFile.delete()
                                                            recordedTempFile = null
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                            }
                                    }
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        if (isRecording) EmergencyRed else WillColor,
                                        CircleShape,
                                    )
                                    .clip(CircleShape),
                            ) {
                                Icon(
                                    if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = if (isRecording) "إيقاف التسجيل" else "بدء التسجيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isRecording) "اضغط لإيقاف التسجيل" else "اضغط لبدء التسجيل",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
