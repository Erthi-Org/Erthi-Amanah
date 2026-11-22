package com.erthi.amanah.ui.screens.more

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun MoreScreen(
    onNavigateEmergency: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateAboutApp: () -> Unit,
    onNavigateAboutErthi: () -> Unit,
    onNavigateFeedback: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { ErthiTopBar(title = "المزيد", onBack = onBack) },
        containerColor = GlossyBlack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Emergency — prominent
            MoreCard(
                icon = Icons.Default.Warning,
                iconColor = EmergencyRed,
                title = "مركز الطوارئ",
                subtitle = "اتصل بسرعة في الحالات الطارئة",
                onClick = onNavigateEmergency,
                highlight = true,
            )

            Spacer(Modifier.height(4.dp))
            SectionLabel("الإعدادات والأمان")
            MoreCard(Icons.Default.Lock, MetallicSilver, "الإعدادات والأمان", "قفل، تشفير، مفتاح الاسترداد", onNavigateSettings)

            Spacer(Modifier.height(4.dp))
            SectionLabel("إرثي")
            MoreCard(Icons.Default.Shield, GoldAccent, "عن إرثي", "مشروع خيري إسلامي مفتوح المصدر", onNavigateAboutErthi)
            MoreCard(Icons.Default.Info, DigitalColor, "عن التطبيق وصيتي", "الإصدار والمعلومات التقنية", onNavigateAboutApp)
            MoreCard(Icons.Default.RateReview, AssetsColor, "شاركنا اقتراحك", "أفكار، تقرير مشكلة، تحسينات", onNavigateFeedback)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MetallicSilver, modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp))
}

@Composable
private fun MoreCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlight: Boolean = false,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (highlight) 1.5.dp else 1.dp,
                color = if (highlight) iconColor.copy(alpha = 0.5f) else MetallicBorder,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0xFF1A0808) else ObsidianCard,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                OrnamentalIcon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            OrnamentalIcon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}
