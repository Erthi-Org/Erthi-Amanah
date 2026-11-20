package com.erthi.amanah.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.BackupState
import com.erthi.amanah.ui.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class PassphraseMode { EXPORT, IMPORT }

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dateStr = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }

    // Local (device-bound) launchers
    val exportLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) else viewModel.clearState() }

    val importLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importBackup(uri) else viewModel.clearState() }

    // Portable (PBKDF2) launchers — uri is captured, then passphrase dialog shown
    var portableExportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var portableImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var passphraseMode by remember { mutableStateOf<PassphraseMode?>(null) }

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

    // Passphrase dialog
    if (passphraseMode != null) {
        PassphraseDialog(
            mode = passphraseMode!!,
            onDismiss = { passphraseMode = null; viewModel.clearState() },
            onConfirm = { passphrase ->
                when (passphraseMode) {
                    PassphraseMode.EXPORT -> portableExportUri?.let { viewModel.exportPortableBackup(it, passphrase) }
                    PassphraseMode.IMPORT -> portableImportUri?.let { viewModel.importPortableBackup(it, passphrase) }
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

            // Status card
            when (val s = state) {
                is BackupState.InProgress -> {
                    Card(colors = CardDefaults.cardColors(containerColor = ObsidianCard), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BackupColor)
                            Spacer(Modifier.width(12.dp))
                            Text("جارٍ المعالجة...", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is BackupState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF061A0E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, SuccessGreenLight.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreenLight, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, color = SuccessGreenLight, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is BackupState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0606)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, EmergencyRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Error, null, tint = EmergencyRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(s.message, color = EmergencyRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                BackupState.Idle -> Unit
            }

            // ── Local (device-bound) section ───────────────────────────────────
            SectionLabel("نسخة محلية (نفس الجهاز فقط)")
            BackupInfoCard(Icons.Default.Security, "التشفير", "AES-256-GCM — مفتاح Android Keystore (مرتبط بالجهاز)", BackupColor)

            Button(
                onClick = { exportLocalLauncher.launch("wasiyyati_local_$dateStr.wasibak") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BackupColor, contentColor = GlossyBlack),
                enabled = state !is BackupState.InProgress,
            ) {
                Icon(Icons.Default.SaveAlt, null)
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
                Icon(Icons.Default.CloudDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("استعادة نسخة محلية", fontWeight = FontWeight.SemiBold)
            }

            // ── Portable (PBKDF2) section ──────────────────────────────────────
            SectionLabel("نسخة محمولة (أي جهاز — بكلمة مرور)")
            BackupInfoCard(Icons.Default.VpnKey, "التشفير", "AES-256-GCM + PBKDF2-SHA256 (310,000 تكرار) — محمية بكلمة مرور", GoldAccent)

            Button(
                onClick = { exportPortableLauncher.launch("wasiyyati_portable_$dateStr.wasi2bak") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = GlossyBlack),
                enabled = state !is BackupState.InProgress,
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("نسخة محمولة (أي جهاز)", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { importPortableLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                enabled = state !is BackupState.InProgress,
            ) {
                Icon(Icons.Default.FileDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("استعادة نسخة محمولة", fontWeight = FontWeight.SemiBold)
            }

            // ── Cloud section (future) ─────────────────────────────────────────
            SectionLabel("مزودو التخزين السحابي (قريباً)")
            CloudProviderRow("Google Drive", Icons.Default.CloudQueue, DigitalColor, false)
            CloudProviderRow("Microsoft OneDrive", Icons.Default.Cloud, Color(0xFF0078D4), false)
            CloudProviderRow("Dropbox", Icons.Default.CloudCircle, Color(0xFF0061FF), false)

            Text(
                "⚠️ النسخة المحلية مرتبطة بهذا الجهاز. للاستعادة على جهاز آخر استخدم النسخة المحمولة واحتفظ بكلمة المرور بأمان.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
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
    val isExport = mode == PassphraseMode.EXPORT
    val mismatch = isExport && passphrase != confirm && confirm.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text(
                if (isExport) "كلمة مرور النسخة المحمولة" else "أدخل كلمة مرور الملف",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (isExport) "ستُستخدم لتشفير النسخة الاحتياطية. احتفظ بها في مكان آمن — لا يمكن استرداد البيانات بدونها."
                    else "أدخل كلمة المرور التي استخدمتها عند إنشاء هذه النسخة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(if (isExport) "كلمة المرور *" else "كلمة المرور *") },
                    singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = MetallicSilver)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MetallicBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
                if (isExport) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("تأكيد كلمة المرور *") },
                        singleLine = true,
                        isError = mismatch,
                        supportingText = if (mismatch) { { Text("كلمتا المرور غير متطابقتان", color = EmergencyRed) } } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = MetallicBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = MetallicSilver) }
        },
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
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MetallicSilver, fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun CloudProviderRow(name: String, icon: ImageVector, iconColor: Color, connected: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(10.dp)),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(
                if (connected) "✓ متصل" else "غير متصل",
                style = MaterialTheme.typography.labelSmall,
                color = if (connected) SuccessGreenLight else TextMuted,
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}
