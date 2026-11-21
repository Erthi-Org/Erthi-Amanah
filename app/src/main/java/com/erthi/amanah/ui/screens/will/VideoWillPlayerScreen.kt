package com.erthi.amanah.ui.screens.will

import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.VideoWillViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VideoWillPlayerScreen(
    encryptedVideoPath: String,
    onBack: () -> Unit,
    viewModel: VideoWillViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tempPlaybackFile by remember { mutableStateOf<File?>(null) }
    var isDecrypting by remember { mutableStateOf(true) }
    var decryptError by remember { mutableStateOf<String?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Decrypt to temp on enter
    LaunchedEffect(encryptedVideoPath) {
        isDecrypting = true
        decryptError = null
        val encFile = File(encryptedVideoPath)
        if (!encFile.exists()) {
            decryptError = "ملف الفيديو المشفر غير موجود"
            isDecrypting = false
            return@LaunchedEffect
        }
        val tmp = viewModel.decryptVideoToTemp(encFile, context.cacheDir)
        if (tmp == null) {
            decryptError = "فشل فك تشفير الفيديو — تأكد أن هذا الجهاز هو جهاز التسجيل الأصلي"
            isDecrypting = false
            return@LaunchedEffect
        }
        tempPlaybackFile = tmp
        val player = ExoPlayer.Builder(context).build().also { p ->
            p.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(tmp)))
            p.prepare()
            exoPlayer = p
        }
        isDecrypting = false
    }

    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
            tempPlaybackFile?.delete()
            tempPlaybackFile = null
        }
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "مشاهدة الوصية المصورة", onBack = {
            exoPlayer?.release()
            exoPlayer = null
            tempPlaybackFile?.delete()
            tempPlaybackFile = null
            onBack()
        }) },
        containerColor = Color.Black,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isDecrypting -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = WillColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("جاري فك تشفير الفيديو...", color = TextPrimary, fontSize = 16.sp)
                    }
                }
                decryptError != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Error, null, tint = EmergencyRed, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            decryptError ?: "خطأ غير معروف",
                            color = TextPrimary, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        ) { Text("رجوع") }
                    }
                }
                else -> {
                    val player = exoPlayer
                    if (player != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).also { pv ->
                                    pv.player = player
                                    pv.useController = true
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
