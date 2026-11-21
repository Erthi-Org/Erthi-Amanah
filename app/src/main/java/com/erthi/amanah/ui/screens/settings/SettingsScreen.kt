package com.erthi.amanah.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.BuildConfig
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateAboutApp: () -> Unit, onNavigateAboutErthi: () -> Unit, onLockApp: () -> Unit = {}) {
    Scaffold(
        topBar = { ErthiTopBar(title = "الإعدادات والأمان", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingItem(Icons.Default.Lock, "قفل التطبيق", "بالبصمة أو الرمز السري", onClick = onLockApp)
            SettingItem(Icons.Default.Security, "تشفير البيانات", "تشفير محلي AES-256", comingSoon = true)
            SettingItem(Icons.Default.VpnKey, "مفتاح الاسترداد", "إدارة مفتاح فك التشفير", comingSoon = true)
            SettingItem(Icons.Default.Description, "إثبات الوفاة (اختياري)", "تحديد الطريقة المطلوبة لفتح الوصية", comingSoon = true)
            SettingItem(Icons.Default.VisibilityOff, "الخصوصية", "لا يتم رفع بياناتك إلى أي خادم", comingSoon = true)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(8.dp))
            SettingItem(Icons.Default.Info, "معلومات عن التطبيق", "إرثي أمانة - مشروع خيري مفتوح المصدر", onClick = onNavigateAboutApp)
            SettingItem(Icons.Default.Favorite, "عن إرثي", "مبادرة إرثي الخيرية", onClick = onNavigateAboutErthi)
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Default.Shield, null, tint = SilverBlue, modifier = Modifier.size(40.dp))
                Text("إرثي أمانة", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Erthi Amanah", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("مشروع خيري تابع لمبادرة إرثي", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("مفتوح المصدر · مجاني · يحترم خصوصيتك", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                Text("الإصدار: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    comingSoon: Boolean = false,
    onClick: () -> Unit = {},
) {
    Card(
        onClick = if (comingSoon) ({}) else onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (comingSoon) NavyCard.copy(alpha = 0.6f) else NavyCard,
        ),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint = if (comingSoon) SilverBlue.copy(alpha = 0.5f) else SilverBlue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (comingSoon) TextPrimary.copy(alpha = 0.5f) else TextPrimary,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (comingSoon) TextSecondary.copy(alpha = 0.5f) else TextSecondary,
                )
            }
            if (comingSoon) {
                Surface(
                    color = MetallicBorder.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        "قريباً",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
