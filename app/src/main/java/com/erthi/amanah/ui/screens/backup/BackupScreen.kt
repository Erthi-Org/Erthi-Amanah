package com.erthi.amanah.ui.screens.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun BackupScreen(onBack: () -> Unit) {
    var cloudEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ErthiTopBar(title = "النسخ الاحتياطي", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("احفظ نسخة مشفرة من بياناتك في مكان آمن", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            BackupOptionRow("النسخ الاحتياطي السحابي", "رفع نسخة مشفرة تلقائياً", cloudEnabled) { cloudEnabled = it }
            ProviderRow("Google Drive", connected = false, Icons.Default.Cloud)
            ProviderRow("OneDrive", connected = false, Icons.Default.CloudQueue)
            ProviderRow("Dropbox", connected = false, Icons.Default.CloudOff)
            BackupInfoRow("تكرار النسخ الاحتياطي", "أسبوعياً", Icons.Default.Schedule)
            BackupInfoRow("آخر نسخة احتياطية", "لم تُنشأ بعد", Icons.Default.CheckCircle)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BackupColor, contentColor = NavyDeep),
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("إنشاء نسخة احتياطية الآن", fontWeight = FontWeight.Bold)
            }
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
            Switch(checked = enabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = NavyDeep, checkedTrackColor = BackupColor))
        }
    }
}

@Composable
private fun ProviderRow(name: String, connected: Boolean, icon: ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = SilverBlue, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(if (connected) "متصل" else "غير متصل", style = MaterialTheme.typography.labelSmall, color = if (connected) SuccessGreenLight else TextMuted)
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
