package com.erthi.amanah.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.BuildConfig
import com.erthi.amanah.ui.components.AndalusianSettingItem
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateAboutApp: () -> Unit,
    onNavigateAboutErthi: () -> Unit,
    onLockApp: () -> Unit = {},
    onNavigatePrivacy: () -> Unit = {},
    onNavigateEncryption: () -> Unit = {},
    onNavigateRecoveryKey: () -> Unit = {},
    onNavigateDeathVerification: () -> Unit = {},
    onNavigateLanguagePicker: () -> Unit = {},
) {
    Scaffold(
        topBar = { ErthiTopBar(title = "الإعدادات والأمان", onBack = onBack) },
        containerColor = AndalusianBlue,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AndalusianSettingItem(
                icon = Icons.Default.Lock,
                iconColor = RoyalGold,
                title = "قفل التطبيق",
                subtitle = "بالبصمة أو الرمز السري",
                onClick = onLockApp,
            )
            AndalusianSettingItem(
                icon = Icons.Default.Security,
                iconColor = SuccessGreenLight,
                title = "تشفير البيانات",
                subtitle = "تشفير محلي AES-256 — مُفعَّل",
                onClick = onNavigateEncryption,
            )
            AndalusianSettingItem(
                icon = Icons.Default.VpnKey,
                iconColor = PurpleEnamelLight,
                title = "مفتاح الاسترداد",
                subtitle = "النسخ الاحتياطي المشفر كمسار للاسترداد",
                badge = "معلومات",
                badgeColor = PurpleEnamelLight,
                onClick = onNavigateRecoveryKey,
            )
            AndalusianSettingItem(
                icon = Icons.Default.Description,
                iconColor = PinkEnamel,
                title = "إثبات الوفاة (اختياري)",
                subtitle = "تحديد طريقة فتح الوصية للمستفيدين",
                badge = "قريباً",
                badgeColor = PinkEnamel,
                onClick = onNavigateDeathVerification,
            )
            AndalusianSettingItem(
                icon = Icons.Default.VisibilityOff,
                iconColor = DigitalColor,
                title = "الخصوصية",
                subtitle = "بياناتك محلية — اعرف كيف تُحفظ",
                onClick = onNavigatePrivacy,
            )
            AndalusianSettingItem(
                icon = Icons.Default.Language,
                iconColor = RoyalGoldLight,
                title = "لغة التطبيق",
                subtitle = "العربية · English · Français · Deutsch · …",
                onClick = onNavigateLanguagePicker,
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            AndalusianSettingItem(
                icon = Icons.Default.Info,
                iconColor = MetallicSilver,
                title = "معلومات عن التطبيق",
                subtitle = "إرثي أمانة - مشروع خيري مفتوح المصدر",
                onClick = onNavigateAboutApp,
            )
            AndalusianSettingItem(
                icon = Icons.Default.Favorite,
                iconColor = PinkEnamel,
                title = "عن إرثي",
                subtitle = "مبادرة إرثي الخيرية",
                onClick = onNavigateAboutErthi,
            )

            Spacer(modifier = Modifier.weight(1f).defaultMinSize(minHeight = 24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Icon(Icons.Default.Shield, null, tint = RoyalGold, modifier = Modifier.size(40.dp))
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
