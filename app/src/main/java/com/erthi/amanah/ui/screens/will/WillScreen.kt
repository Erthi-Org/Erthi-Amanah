package com.erthi.amanah.ui.screens.will

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun WillScreen(onBack: () -> Unit, onEditWill: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "الوصية", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavyCard),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("اكتب وصيتك وعيّن آلية الوصول إليها", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("اكتب وصيتك", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SilverBlue, modifier = Modifier.padding(bottom = 12.dp))
            WillOptionItem(icon = Icons.Default.Edit, label = "كتابة الوصية", onClick = onEditWill)
            WillOptionItem(icon = Icons.Default.Mic, label = "تسجيل وصية صوتية", onClick = {})
            WillOptionItem(icon = Icons.Default.Videocam, label = "تسجيل وصية بالفيديو", onClick = {})
            WillOptionItem(icon = Icons.Default.AttachFile, label = "إرفاق مستندات", onClick = {})
            WillOptionItem(icon = Icons.Default.Key, label = "إعدادات الوصول\nمن يمكنه فتح الوصية وكيف؟", onClick = {})
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1A1200)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنبيه: الوصية المكتوبة هنا هي توثيق شخصي. مدى إلزاميتها القانونية يعتمد على تشريعات بلدك وشروط الإشهاد الرسمي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun WillOptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SilverBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}
