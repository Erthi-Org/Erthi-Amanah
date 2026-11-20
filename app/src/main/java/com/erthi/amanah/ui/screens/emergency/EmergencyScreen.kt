package com.erthi.amanah.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

data class EmergencyNumber(val number: String, val label: String, val icon: ImageVector)

@Composable
fun EmergencyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val numbers = listOf(
        EmergencyNumber("123", "الإسعاف", Icons.Default.LocalHospital),
        EmergencyNumber("122", "الشرطة", Icons.Default.LocalPolice),
        EmergencyNumber("180", "المطافئ", Icons.Default.LocalFireDepartment),
        EmergencyNumber("112", "طوارئ عامة", Icons.Default.Warning),
    )

    fun dial(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "مركز الطوارئ", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF3A0808)).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = EmergencyRed, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("اتصل بسرعة في الحالات الطارئة", color = EmergencyRed, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("أرقام الطوارئ — مصر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SilverBlue)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                numbers.take(2).forEach { n ->
                    EmergencyNumberCard(n, modifier = Modifier.weight(1f), onClick = { dial(n.number) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                numbers.drop(2).forEach { n ->
                    EmergencyNumberCard(n, modifier = Modifier.weight(1f), onClick = { dial(n.number) })
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("جهات اتصال للطوارئ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SilverBlue)
            Spacer(Modifier.height(8.dp))
            EmptyContactPlaceholder()
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
            ) {
                Icon(Icons.Default.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text("إرسال استغاثة وموقعي\nإلى جهات الاتصال المحددة", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun EmergencyNumberCard(n: EmergencyNumber, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0808)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(n.icon, null, tint = EmergencyRed, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text(n.number, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
            Text(n.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmptyContactPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PersonAdd, null, tint = SilverBlue)
            Spacer(Modifier.width(10.dp))
            Text("إضافة جهة اتصال للطوارئ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
