package com.erthi.amanah.ui.screens.will

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.WillEntity
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.WillViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WillScreen(
    onBack: () -> Unit,
    onEditWill: () -> Unit,
    onWillAccess: () -> Unit = {},
    onAudioWill: () -> Unit = {},
    onVideoWill: () -> Unit = {},
    onPlayVideo: (String) -> Unit = {},
    viewModel: WillViewModel = hiltViewModel(),
) {
    val wills by viewModel.wills.collectAsState()

    Scaffold(
        topBar = { ErthiTopBar(title = "الوصية", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .clip(RoundedCornerShape(16.dp)).background(NavyCard),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("اكتب وصيتك وعيّن آلية الوصول إليها", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Text("إجراءات الوصية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SilverBlue, modifier = Modifier.padding(bottom = 4.dp))
            }
            item { WillOptionItem(icon = Icons.Default.Edit, label = "كتابة وصية جديدة", onClick = onEditWill) }
            item { WillOptionItem(icon = Icons.Default.Mic, label = "تسجيل وصية صوتية", onClick = onAudioWill) }
            item { WillOptionItem(icon = Icons.Default.Videocam, label = "تسجيل وصية مصورة", onClick = onVideoWill) }
            item { WillOptionItem(icon = Icons.Default.Key, label = "إعدادات الوصول", onClick = onWillAccess) }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1A1200)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "تنبيه: الوصية المكتوبة هنا هي توثيق شخصي. مدى إلزاميتها القانونية يعتمد على تشريعات بلدك.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                        )
                    }
                }
            }
            if (wills.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("الوصايا المحفوظة (${wills.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SilverBlue, modifier = Modifier.padding(bottom = 4.dp))
                }
                items(wills, key = { it.id }) { will ->
                    WillCard(will = will, onDelete = { viewModel.deleteWill(will) }, onPlayVideo = onPlayVideo)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WillCard(will: WillEntity, onDelete: () -> Unit, onPlayVideo: ((String) -> Unit)? = null) {
    val dateStr = remember(will.updatedAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(will.updatedAt))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (will.isDraft) Icons.Default.EditNote else Icons.Default.HistoryEdu,
                contentDescription = null,
                tint = if (will.isDraft) TextSecondary else GoldAccent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(will.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(will.content, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (will.isDraft) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = SilverBlue.copy(alpha = 0.2f)) {
                            Text("مسودة", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = SilverBlue)
                        }
                    }
                    if (will.videoPath != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = WillColor.copy(alpha = 0.2f)) {
                            Text("مصورة", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = WillColor)
                        }
                    }
                }
            }
            if (will.videoPath != null && onPlayVideo != null) {
                IconButton(onClick = { onPlayVideo(will.videoPath!!) }) {
                    Icon(Icons.Default.PlayCircle, null, tint = WillColor, modifier = Modifier.size(24.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp))
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
