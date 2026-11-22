package com.erthi.amanah.ui.screens.will

import com.erthi.amanah.ui.components.OrnamentalIcon
import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.VideoWillState
import com.erthi.amanah.ui.viewmodel.VideoWillViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoWillScreen(
    onBack: () -> Unit,
    viewModel: VideoWillViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()

    // ── Permissions ──────────────────────────────────────────────────────────
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
        hasMicPermission   = results[Manifest.permission.RECORD_AUDIO] == true
    }

    // ── Recording state ───────────────────────────────────────────────────────
    var isRecording        by remember { mutableStateOf(false) }
    var durationSec        by remember { mutableStateOf(0) }
    var currentRecording   by remember { mutableStateOf<Recording?>(null) }
    var recordedTempFile   by remember { mutableStateOf<File?>(null) }
    var cameraFacing       by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var showCancelConfirm  by remember { mutableStateOf(false) }
    var showSaveDialog     by remember { mutableStateOf(false) }
    var saveTitle          by remember { mutableStateOf("") }
    var videoCapture       by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraProvider     by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Fix 5: store PreviewView ref so LaunchedEffect can bind camera without re-entering update{}
    var previewViewRef     by remember { mutableStateOf<PreviewView?>(null) }

    val availableMb = remember { context.filesDir.freeSpace / (1024 * 1024) }

    // Duration counter — only increments while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            durationSec = 0
            while (isRecording) { delay(1000L); durationSec++ }
        }
    }

    // Cleanup stale temp files from previous sessions
    LaunchedEffect(Unit) { viewModel.cleanupTempFiles() }

    // P0.3: include previewViewRef so camera re-binds when PreviewView is recreated
    LaunchedEffect(cameraFacing, hasCameraPermission, hasMicPermission, previewViewRef) {
        val pv = previewViewRef ?: return@LaunchedEffect
        if (!hasCameraPermission || !hasMicPermission) return@LaunchedEffect
        // Guard: don't unbind while recording is active (would abort the recording)
        if (isRecording) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.fromOrderedList(
                    listOf(Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                .build()
            val vc = VideoCapture.withOutput(recorder)
            videoCapture = vc
            val selector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            } catch (_: Exception) {
                // Camera unavailable — user sees no preview; they can retry
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Fix 5: release camera resources when this composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    // Fix 5: helper — request stop without touching isRecording (Finalize event sets it)
    fun requestStopRecording() {
        currentRecording?.stop()
        // isRecording is set to false inside VideoRecordEvent.Finalize, not here
        currentRecording = null
    }

    // ── Cancel confirmation dialog ────────────────────────────────────────────
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            containerColor = NavyCard,
            title = { Text("إلغاء التسجيل", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text("هل تريد إلغاء التسجيل وحذف هذا الفيديو؟", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    requestStopRecording()
                    recordedTempFile?.delete(); recordedTempFile = null
                    showCancelConfirm = false
                    onBack()
                }) { Text("إلغاء وحذف", color = EmergencyRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("استمرار", color = WillColor) }
            },
        )
    }

    // Fix 1: Save dialog calls encryptAndSave — Room only ever receives the encrypted path
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
                    Text("سيتم تشفير الفيديو بأمان قبل الحفظ.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tempFile = recordedTempFile
                        if (tempFile != null) {
                            // Fix 1: encrypt first → then insert Room record with encrypted path
                            viewModel.encryptAndSave(
                                title = saveTitle.ifBlank { "وصية مصورة" },
                                sourceFile = tempFile,
                            )
                        }
                        showSaveDialog = false
                        recordedTempFile = null
                    },
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

            // ── Permission screen ─────────────────────────────────────────────
            if (!hasCameraPermission || !hasMicPermission) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OrnamentalIcon(Icons.Default.Videocam, null, tint = WillColor, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("تسجيل الوصية المصورة",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("يحتاج التطبيق إلى إذن الكاميرا والميكروفون.",
                        color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            permissionsLauncher.launch(arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WillColor),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("منح الصلاحيات", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                return@Scaffold
            }

            // Fix 5: AndroidView factory only sets up the PreviewView; binding happens in
            // LaunchedEffect(cameraFacing) above — never inside update{} to avoid
            // unbindAll() on every recomposition (which would abort an active recording).
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        pv.scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewRef = pv
                    }
                },
                // update{} intentionally empty — camera binding is managed by LaunchedEffect
                modifier = Modifier.fillMaxSize(),
            )

            // ── Top overlay: duration + storage warning ───────────────────────
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
                        Box(Modifier.size(10.dp).background(EmergencyRed, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(formatDuration(durationSec),
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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

            // ── State overlays (encrypting, done, error) ─────────────────────
            when (val s = state) {
                is VideoWillState.Encrypting -> {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = WillColor)
                            Spacer(Modifier.height(12.dp))
                            Text("جاري تشفير الفيديو...", color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
                is VideoWillState.Ready -> {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OrnamentalIcon(Icons.Default.CheckCircle, null,
                                tint = SuccessGreenLight, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("تم حفظ الوصية المصورة بأمان",
                                color = TextPrimary, fontSize = 16.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetState(); onBack() },
                                colors = ButtonDefaults.buttonColors(containerColor = WillColor),
                            ) { Text("العودة", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                is VideoWillState.Error -> {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)) {
                            OrnamentalIcon(Icons.Default.Error, null,
                                tint = EmergencyRed, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(s.message, color = TextPrimary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.resetState() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                else -> {}
            }

            // ── Bottom controls ───────────────────────────────────────────────
            if (state is VideoWillState.Idle) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (recordedTempFile != null) {
                        // After recording: Save or Discard
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { recordedTempFile?.delete(); recordedTempFile = null },
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
                        if (!isRecording) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(
                                    onClick = {
                                        // Fix 5: guard camera switch during recording
                                        if (!isRecording) {
                                            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_FRONT)
                                                CameraSelector.LENS_FACING_BACK
                                            else CameraSelector.LENS_FACING_FRONT
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                ) {
                                    OrnamentalIcon(Icons.Default.Cameraswitch, null,
                                        tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        Box(contentAlignment = Alignment.Center) {
                            if (isRecording) {
                                Box(Modifier.size(88.dp).border(3.dp,
                                    EmergencyRed.copy(alpha = 0.5f), CircleShape))
                            }
                            IconButton(
                                onClick = {
                                    if (isRecording) {
                                        // Fix 5: don't set isRecording=false here — Finalize event does it
                                        requestStopRecording()
                                    } else {
                                        val vc = videoCapture ?: return@IconButton
                                        val tempFile = viewModel.createTempRecordingFile()
                                        recordedTempFile = tempFile
                                        currentRecording = vc.output
                                            .prepareRecording(context, FileOutputOptions.Builder(tempFile).build())
                                            .withAudioEnabled()
                                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                                when (event) {
                                                    // Fix 5: set isRecording=true only on confirmed Start
                                                    is VideoRecordEvent.Start -> { isRecording = true }
                                                    is VideoRecordEvent.Finalize -> {
                                                        // Fix 5: set isRecording=false here, not in stopRecording()
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
                                    .background(if (isRecording) EmergencyRed else WillColor, CircleShape)
                                    .clip(CircleShape),
                            ) {
                                OrnamentalIcon(
                                    if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = if (isRecording) "إيقاف التسجيل" else "بدء التسجيل",
                                    tint = Color.White, modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isRecording) "اضغط لإيقاف التسجيل" else "اضغط لبدء التسجيل",
                            color = Color.White, style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
