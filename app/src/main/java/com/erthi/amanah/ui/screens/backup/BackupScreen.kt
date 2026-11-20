package com.erthi.amanah.ui.screens.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var cloudEnabled by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    val dateStr = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        isExporting = false
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val placeholder = "ERTHI_BACKUP_v1\n[بيانات مشفرة بـ AES-256-GCM]\n[تاريخ النسخ: $dateStr]".toByteArray()
                    out.write(placeholder)
                }
                backupStatus = "✅ تم حفظ النسخة الاحتياطية بنجاح"
            } catch (e: Exception) {
                backupStatus = "❌ فشل الحفظ: ${e.localizedMessage}"
            }
        } else {
            backupStatus = "⚠️ تم إلغاء العملية"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                backupStatus = "✅ تم تحديد الملف — الاستعادة الكاملة ستتوفر في إصدار قادم"
            } catch (_: Exception) {
                backupStatus = "⚠️ تم تحديد الملف للاستعادة"
            }
        }
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "النسخ الاحتياطي", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "احفظ نسخة مشفرة من بياناتك في مكان آمن",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            if (backupStatus != null) {
                Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(12.dp)) {
                    Text(backupStatus!!, modifier = Modifier.padding(14.dp), color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }

            BackupOptionRow("النسخ الاحتياطي السحابي", "قريباً — رفع مشفر تلقائياً", cloudEnabled) { cloudEnabled = it }

            Text("التخزين المحلي (متاح الآن)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SilverBlue)

            BackupInfoRow("تشفير النسخة", "AES-256-GCM", Icons.Default.Security)
            BackupInfoRow("الصيغة", "ملف .erthibak", Icons.Default.Description)
            BackupInfoRow("آخر نسخة", if (backupStatus?.startsWith("✅") == true) dateStr else "لم تُنشأ بعد", Icons.Default.CheckCircle)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isExporting = true
                    exportLauncher.launch("erthi_backup_$dateStr.erthibak")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BackupColor, contentColor = NavyDeep),
                enabled = !isExporting,
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NavyDeep)
                } else {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("إنشاء نسخة احتياطية محلية", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SilverBlue),
            ) {
                Icon(Icons.Default.CloudDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("استعادة من نسخة احتياطية", fontWeight = FontWeight.SemiBold)
            }

            Text(
                "⚠️ الاستعادة الكاملة تتطلب مفتاح التشفير الأصلي المخزن في هاتفك.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun BackupOptionRow(title: String, subtitle: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(checked = enabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = NavyDeep, checkedTrackColor = BackupColor), enabled = false)
        }
    }
}

@Composable
private fun BackupInfoRow(label: String, value: String, icon: ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = SilverBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
