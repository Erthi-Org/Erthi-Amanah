package com.erthi.amanah.ui.screens.backup

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.erthi.amanah.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.backup.DriveBackupFile
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.BackupState
import com.erthi.amanah.ui.viewmodel.BackupViewModel
import com.erthi.amanah.ui.viewmodel.DriveState
import java.text.SimpleDateFormat
import java.util.*

private enum class PassphraseMode { EXPORT, IMPORT, DRIVE_EXPORT, DRIVE_IMPORT }

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val hasPendingRestore by viewModel.hasPendingRestore.collectAsState(initial = false)
    val driveState by viewModel.driveState.collectAsState()
    val driveAccount by viewModel.driveAccount.collectAsState()
    val driveBackups by viewModel.driveBackups.collectAsState()
    val dateStr = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }

    // Local (device-bound) launchers
    val exportLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) else viewModel.clearState() }

    val importLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importBackup(uri) else viewModel.clearState() }

    // Portable (PBKDF2) launchers
    var portableExportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var portableImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var passphraseMode by remember { mutableStateOf<PassphraseMode?>(null) }
    var driveRestoreFileId by remember { mutableStateOf<String?>(null) }

    val exportPortableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) { portableExportUri = uri; passphraseMode = PassphraseMode.EXPORT }
        else viewModel.clearState()
    }

    val importPortableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) { portableImportUri = uri; passphraseMode = PassphraseMode.IMPORT }
        else viewModel.clearState()
    }

    // Google Drive sign-in launcher
    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.handleDriveSignInResult(result.data)
    }

    // Passphrase dialog (shared by portable + Drive flows)
    if (passphraseMode != null) {
        PassphraseDialog(
            mode = passphraseMode!!,
            onDismiss = {
                passphraseMode = null
                driveRestoreFileId = null
                viewModel.clearState()
            },
            onConfirm = { passphrase ->
                when (passphraseMode) {
                    PassphraseMode.EXPORT -> portableExportUri?.let { viewModel.exportPortableBackup(it, passphrase) }
                    PassphraseMode.IMPORT -> portableImportUri?.let { viewModel.importPortableBackup(it, passphrase) }
                    PassphraseMode.DRIVE_EXPORT -> viewModel.exportDriveBackup(passphrase)
                    PassphraseMode.DRIVE_IMPORT -> driveRestoreFileId?.let { viewModel.importDriveBackup(it, passphrase) }
                    null -> Unit
                }
                passphraseMode = null
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "النسخ الاحتياطي", onBack = onBack) },
        containerColor = GlossyBlack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "احفظ نسخة مشفرة من جميع بياناتك في مكان آمن",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            // ── Local backup status card ───────────────────────────────────
            when (val s = state) {
                is BackupState.InProgress -> StatusCard(isLoading = true, message = "جارٍ المعالجة...", isError = false)
                is BackupState.Success -> StatusCard(message = s.message, isError = false)
                is BackupState.Error -> StatusCard(message = s.message, isError = true)
                BackupState.Idle -> Unit
            }

            // ── Local (device-bound) ───────────────────────────────────────
            if (hasPendingRestore) {
                Text(stringResource(R.string.restore_pending_warning), color = RoyalGoldLight)
            }

            SectionLabel("نسخة محلية (نفس الجهاز فقط)")
            BackupInfoCard(Icons.Default.Security, "التشفير", "AES-256-GCM — مفتاح Android Keystore (مرتبط بالجهاز)", BackupColor)
            Button(
                onClick = { exportLocalLauncher.launch("wasiyyati_local_$dateStr.wasibak") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BackupColor, contentColor = GlossyBlack),
                enabled = state !is BackupState.InProgress,
            ) {
                OrnamentalIcon(Icons.Default.SaveAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("نسخة محلية (نفس الجهاز)", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { importLocalLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicSilver),
                enabled = state !is BackupState.InProgress,
            ) {
                OrnamentalIcon(Icons.Default.CloudDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("استعادة نسخة محلية", fontWeight = FontWeight.SemiBold)
            }

            // ── Portable (PBKDF2) ──────────────────────────────────────────
            SectionLabel("نسخة مشفرة بكلمة مرور")
            BackupInfoCard(Icons.Default.VpnKey, "التشفير", "AES-256-GCM + PBKDF2-SHA256 (310,000 تكرار) — محمية بكلمة مرور", GoldAccent)
            Button(
                onClick = { exportPortableLauncher.launch("wasiyyati_portable_$dateStr.wasi2bak") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = GlossyBlack),
                enabled = state !is BackupState.InProgress,
            ) {
                OrnamentalIcon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("نسخة مشفرة بكلمة مرور", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { importPortableLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                enabled = state !is BackupState.InProgress,
            ) {
                OrnamentalIcon(Icons.Default.FileDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("استعادة نسخة محمولة", fontWeight = FontWeight.SemiBold)
            }

            // ── Google Drive section ───────────────────────────────────────
            SectionLabel("Google Drive — سحابي مشفر")

            // Drive status feedback card
            when (val ds = driveState) {
                is DriveState.InProgress -> StatusCard(isLoading = true, message = "جارٍ الاتصال بـ Drive...", isError = false)
                is DriveState.Success -> {
                    StatusCard(message = ds.message, isError = false)
                    LaunchedEffect(ds) { kotlinx.coroutines.delay(4000); viewModel.clearDriveState() }
                }
                is DriveState.Error -> StatusCard(message = ds.message, isError = true)
                DriveState.Idle -> Unit
            }

            if (driveAccount == null) {
                // Not connected
                DriveConnectCard(
                    onConnect = { driveSignInLauncher.launch(viewModel.driveSignInIntent()) },
                )
            } else {
                // Connected — show account + actions
                DriveConnectedCard(
                    email = driveAccount!!.email ?: "",
                    onDisconnect = { viewModel.driveSignOut() },
                    onUpload = { passphraseMode = PassphraseMode.DRIVE_EXPORT },
                    onRefresh = { viewModel.loadDriveBackups() },
                    isLoading = driveState is DriveState.InProgress,
                )
                // Backup list
                if (driveBackups.isNotEmpty()) {
                    driveBackups.forEach { backup ->
                        DriveBackupItem(
                            backup = backup,
                            onRestore = {
                                driveRestoreFileId = backup.id
                                passphraseMode = PassphraseMode.DRIVE_IMPORT
                            },
                            onDelete = { viewModel.deleteDriveBackup(backup.id) },
                        )
                    }
                } else if (driveState is DriveState.Idle) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            OrnamentalIcon(Icons.Default.CloudOff, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("لا توجد نسخ احتياطية على Drive", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Notes
            Text(
                "النسخة المحلية مرتبطة بمفتاح هذا التثبيت. استعادة النسخة المشفرة بكلمة مرور على جهاز آخر قيد التحقق.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

// ── Drive-specific composables ────────────────────────────────────────────────

@Composable
private fun DriveConnectCard(onConnect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061218)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DigitalColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(DigitalColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    OrnamentalIcon(Icons.Default.CloudQueue, null, tint = DigitalColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Google Drive", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("نسخة احتياطية مشفرة على حسابك الشخصي", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            BackupInfoCard(
                icon = Icons.Default.Security,
                label = "التشفير",
                value = "نفس تشفير النسخة المحمولة (PBKDF2+AES-256-GCM) قبل الرفع",
                iconColor = DigitalColor,
            )
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DigitalColor, contentColor = Color.White),
            ) {
                OrnamentalIcon(Icons.Default.Login, null)
                Spacer(Modifier.width(8.dp))
                Text("تسجيل الدخول بـ Google", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DriveConnectedCard(
    email: String,
    onDisconnect: () -> Unit,
    onUpload: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF061218)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DigitalColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(DigitalColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    OrnamentalIcon(Icons.Default.CloudDone, null, tint = DigitalColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("متصل بـ Google Drive", fontWeight = FontWeight.Bold, color = DigitalColor, style = MaterialTheme.typography.bodyMedium)
                    Text(email, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    OrnamentalIcon(Icons.Default.Refresh, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUpload,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DigitalColor, contentColor = Color.White),
                    enabled = !isLoading,
                ) {
                    OrnamentalIcon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("رفع نسخة", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                ) {
                    OrnamentalIcon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("قطع", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DriveBackupItem(
    backup: DriveBackupFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateStr = remember(backup.modifiedTimeMs) {
        SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault()).format(Date(backup.modifiedTimeMs))
    }
    val sizeStr = when {
        backup.sizeBytes > 1_048_576 -> "${backup.sizeBytes / 1_048_576} ميجابايت"
        backup.sizeBytes > 1_024 -> "${backup.sizeBytes / 1_024} كيلوبايت"
        else -> "${backup.sizeBytes} بايت"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(12.dp)),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OrnamentalIcon(Icons.Default.CloudDone, null, tint = DigitalColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(backup.name, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$dateStr · $sizeStr", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(32.dp)) {
                OrnamentalIcon(Icons.Default.Restore, null, tint = DigitalColor, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                OrnamentalIcon(Icons.Default.Delete, null, tint = EmergencyRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    message: String,
    isError: Boolean,
    isLoading: Boolean = false,
) {
    val (bg, border, tint) = when {
        isLoading -> Triple(ObsidianCard, MetallicBorder, BackupColor)
        isError -> Triple(Color(0xFF1A0606), EmergencyRed.copy(alpha = 0.4f), EmergencyRed)
        else -> Triple(Color(0xFF061A0E), SuccessGreenLight.copy(alpha = 0.4f), SuccessGreenLight)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, border, RoundedCornerShape(12.dp)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = tint)
            } else {
                OrnamentalIcon(
                    if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                    null, tint = tint, modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(message, color = tint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PassphraseDialog(
    mode: PassphraseMode,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val isExport = mode in listOf(PassphraseMode.EXPORT, PassphraseMode.DRIVE_EXPORT)
    val mismatch = isExport && passphrase != confirm && confirm.isNotEmpty()
    val titleText = when (mode) {
        PassphraseMode.EXPORT, PassphraseMode.DRIVE_EXPORT -> "كلمة مرور النسخة المشفرة"
        PassphraseMode.IMPORT, PassphraseMode.DRIVE_IMPORT -> "أدخل كلمة مرور الملف"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text(titleText, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (isExport) "ستُستخدم لتشفير النسخة الاحتياطية. احتفظ بها في مكان آمن — لا يمكن استرداد البيانات بدونها."
                    else "أدخل كلمة المرور التي استخدمتها عند إنشاء هذه النسخة.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                )
                OutlinedTextField(
                    value = passphrase, onValueChange = { passphrase = it },
                    label = { Text("كلمة المرور *") }, singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            OrnamentalIcon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = MetallicSilver)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent, unfocusedBorderColor = MetallicBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    ),
                )
                if (isExport) {
                    OutlinedTextField(
                        value = confirm, onValueChange = { confirm = it },
                        label = { Text("تأكيد كلمة المرور *") }, singleLine = true,
                        isError = mismatch,
                        supportingText = if (mismatch) { { Text("كلمتا المرور غير متطابقتان", color = EmergencyRed) } } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent, unfocusedBorderColor = MetallicBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (passphrase.isNotBlank()) onConfirm(passphrase) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = GlossyBlack),
                enabled = passphrase.length >= 6 && (!isExport || (passphrase == confirm)),
            ) { Text(if (isExport) "تشفير وحفظ" else "فك التشفير") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = MetallicSilver) } },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MetallicSilver, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
private fun BackupInfoCard(icon: ImageVector, label: String, value: String, iconColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(10.dp)),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            OrnamentalIcon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MetallicSilver, fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
