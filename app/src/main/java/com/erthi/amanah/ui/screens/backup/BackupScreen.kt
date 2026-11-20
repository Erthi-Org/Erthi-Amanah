package com.erthi.amanah.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.BackupState
import com.erthi.amanah.ui.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dateStr = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
        else viewModel.clearState()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
        else viewModel.clearState()
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

            // Local storage section
            SectionLabel("التخزين المحلي")
            BackupInfoCard(Icons.Default.Security, "التشفير", "AES-256-GCM — مفتاح Android Keystore", BackupColor)
            BackupInfoCard(Icons.Default.Description, "صيغة الملف", "وصيتي.wasibak — مشفّر ومعنوَن", BackupColor)
            BackupInfoCard(Icons.Default.DataObject, "يشمل البيانات", "الوصايا، الديون، الممتلكات، المستفيدون، الحسابات الرقمية، كلمات المرور", BackupColor)

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { exportLauncher.launch("wasiyyati_backup_$dateStr.wasibak") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BackupColor, contentColor = GlossyBlack),
                enabled = state !is BackupState.InProgress,
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("إنشاء نسخة احتياطية محلية", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicSilver),
                enabled = state !is BackupState.InProgress,
            ) {
                Icon(Icons.Default.CloudDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("استعادة من نسخة احتياطية", fontWeight = FontWeight.SemiBold)
            }

            // Cloud section (future)
            SectionLabel("مزودو التخزين السحابي (قريباً)")
            CloudProviderRow("Google Drive", Icons.Default.CloudQueue, DigitalColor, false)
            CloudProviderRow("Microsoft OneDrive", Icons.Default.Cloud, Color(0xFF0078D4), false)
            CloudProviderRow("Dropbox", Icons.Default.CloudCircle, Color(0xFF0061FF), false)

            Text(
                "⚠️ مفتاح التشفير مرتبط بهذا الجهاز. الاستعادة على جهاز آخر تتطلب مفتاح الاسترداد.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
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
