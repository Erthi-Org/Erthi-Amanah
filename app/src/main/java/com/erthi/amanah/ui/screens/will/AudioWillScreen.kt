package com.erthi.amanah.ui.screens.will

import com.erthi.amanah.ui.components.OrnamentalIcon
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.AudioWillViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private enum class RecordState { IDLE, RECORDING, STOPPED, PLAYING, SAVED }

@Composable
fun AudioWillScreen(
    onBack: () -> Unit,
    viewModel: AudioWillViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recordState by remember { mutableStateOf(RecordState.IDLE) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var savedFileName by remember { mutableStateOf<String?>(null) }
    var encryptedFile by remember { mutableStateOf<File?>(null) }
    var tempPlaybackFile by remember { mutableStateOf<File?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val outputFile = remember {
        File(context.filesDir, "audio_wills").also { it.mkdirs() }
            .let { dir ->
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                File(dir, "will_audio_$ts.3gp")
            }
    }

    LaunchedEffect(recordState) {
        if (recordState == RecordState.RECORDING) {
            while (isActive && recordState == RecordState.RECORDING) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.value?.apply { try { stop(); release() } catch (_: Exception) {} }
            player.value?.apply { try { stop(); release() } catch (_: Exception) {} }
            tempPlaybackFile?.delete()
        }
    }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) errorMsg = "يلزم منح إذن التسجيل الصوتي"
    }

    fun startRecording() {
        try {
            @Suppress("DEPRECATION")
            val mr = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mr.setOutputFile(outputFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder.value = mr
            elapsedSeconds = 0
            recordState = RecordState.RECORDING
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "فشل بدء التسجيل: ${e.localizedMessage}"
        }
    }

    fun stopRecording() {
        try {
            recorder.value?.apply { stop(); release() }
            recorder.value = null
            recordState = RecordState.STOPPED
        } catch (e: Exception) {
            errorMsg = "فشل إيقاف التسجيل: ${e.localizedMessage}"
        }
    }

    fun startPlayback() {
        scope.launch {
            try {
                val sourceFile = encryptedFile ?: outputFile
                val playFile = if (encryptedFile != null) {
                    val tmp = viewModel.decryptAudioToTemp(sourceFile, context.cacheDir)
                    if (tmp == null) {
                        errorMsg = "فشل فك تشفير التسجيل"
                        return@launch
                    }
                    tempPlaybackFile = tmp
                    tmp
                } else {
                    sourceFile
                }
                val mp = MediaPlayer()
                mp.setDataSource(playFile.absolutePath)
                mp.prepare()
                mp.setOnCompletionListener {
                    recordState = if (encryptedFile != null) RecordState.SAVED else RecordState.STOPPED
                    tempPlaybackFile?.delete()
                    tempPlaybackFile = null
                    player.value = null
                }
                mp.start()
                player.value = mp
                recordState = RecordState.PLAYING
            } catch (e: Exception) {
                errorMsg = "فشل تشغيل التسجيل: ${e.localizedMessage}"
            }
        }
    }

    fun stopPlayback() {
        player.value?.apply { try { stop(); release() } catch (_: Exception) {} }
        player.value = null
        tempPlaybackFile?.delete()
        tempPlaybackFile = null
        recordState = if (encryptedFile != null) RecordState.SAVED else RecordState.STOPPED
    }

    fun saveRecording() {
        scope.launch {
            val enc = viewModel.encryptAudio(outputFile)
            if (enc != null) {
                encryptedFile = enc
                savedFileName = enc.name
                recordState = RecordState.SAVED
            } else {
                errorMsg = "فشل تشفير التسجيل"
            }
        }
    }

    fun discardRecording() {
        encryptedFile?.delete()
        encryptedFile = null
        if (outputFile.exists()) outputFile.delete()
        savedFileName = null
        elapsedSeconds = 0
        recordState = RecordState.IDLE
        errorMsg = null
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timerText = "%02d:%02d".format(minutes, seconds)

    Scaffold(
        topBar = { ErthiTopBar(title = "تسجيل وصية صوتية", onBack = onBack) },
        containerColor = GlossyBlack,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D0A00)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    OrnamentalIcon(Icons.Default.Info, null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("سجّل وصيتك بصوتك. يُخزَّن التسجيل محلياً على هاتفك مشفّراً بـ AES-256.", style = MaterialTheme.typography.bodySmall, color = GoldAccent.copy(alpha = 0.85f))
                }
            }

            Spacer(Modifier.weight(1f))

            when (recordState) {
                RecordState.RECORDING -> {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(EmergencyRed))
                    Text("جارٍ التسجيل", color = EmergencyRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                RecordState.PLAYING -> {
                    OrnamentalIcon(Icons.Default.PlayCircle, null, tint = VaultColor, modifier = Modifier.size(32.dp))
                    Text("جارٍ التشغيل", color = VaultColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                RecordState.STOPPED -> {
                    OrnamentalIcon(Icons.Default.CheckCircle, null, tint = SuccessGreenLight, modifier = Modifier.size(32.dp))
                    Text("اكتمل التسجيل", color = SuccessGreenLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                RecordState.SAVED -> {
                    OrnamentalIcon(Icons.Default.SaveAlt, null, tint = GoldAccent, modifier = Modifier.size(32.dp))
                    Text("تم الحفظ المشفّر", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                else -> {
                    OrnamentalIcon(Icons.Default.Mic, null, tint = MetallicSilver, modifier = Modifier.size(48.dp))
                    Text("اضغط للبدء", color = TextSecondary, fontSize = 16.sp)
                }
            }

            Text(
                timerText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = when (recordState) {
                    RecordState.RECORDING -> EmergencyRed
                    RecordState.PLAYING -> VaultColor
                    RecordState.SAVED -> GoldAccent
                    else -> TextSecondary
                },
            )

            errorMsg?.let {
                Text(it, color = EmergencyRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }

            savedFileName?.let {
                Text("محفوظ: $it", color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f))

            if (!hasPermission) {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = GlossyBlack),
                ) {
                    OrnamentalIcon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(8.dp))
                    Text("منح إذن التسجيل", fontWeight = FontWeight.Bold)
                }
            } else {
                when (recordState) {
                    RecordState.IDLE -> {
                        Button(
                            onClick = { startRecording() },
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            OrnamentalIcon(Icons.Default.Mic, null, modifier = Modifier.size(36.dp))
                        }
                    }
                    RecordState.RECORDING -> {
                        Button(
                            onClick = { stopRecording() },
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = EmergencyRed),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            OrnamentalIcon(Icons.Default.Stop, null, modifier = Modifier.size(36.dp))
                        }
                    }
                    RecordState.STOPPED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { startPlayback() },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColor),
                            ) {
                                OrnamentalIcon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text("استماع")
                            }
                            Button(
                                onClick = { saveRecording() },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = GlossyBlack),
                            ) {
                                OrnamentalIcon(Icons.Default.Save, null)
                                Spacer(Modifier.width(6.dp))
                                Text("حفظ مشفّر", fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { discardRecording() }, modifier = Modifier.fillMaxWidth()) {
                            Text("تجاهل وإعادة التسجيل", color = EmergencyRed)
                        }
                    }
                    RecordState.PLAYING -> {
                        Button(
                            onClick = { stopPlayback() },
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = VaultColor, contentColor = Color.White),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            OrnamentalIcon(Icons.Default.Stop, null, modifier = Modifier.size(36.dp))
                        }
                    }
                    RecordState.SAVED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { startPlayback() },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColor),
                            ) {
                                OrnamentalIcon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text("استماع")
                            }
                            Button(
                                onClick = { onBack() },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = GlossyBlack),
                            ) {
                                OrnamentalIcon(Icons.Default.Check, null)
                                Spacer(Modifier.width(6.dp))
                                Text("تم", fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { discardRecording() }, modifier = Modifier.fillMaxWidth()) {
                            Text("حذف التسجيل", color = EmergencyRed)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
