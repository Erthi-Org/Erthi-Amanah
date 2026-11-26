package com.erthi.amanah.ui.screens.settings

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.erthi.amanah.R
import com.erthi.amanah.ui.viewmodel.AuthViewModel
import com.erthi.amanah.ui.components.AndalusianCard
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
    authViewModel: AuthViewModel,
    onNavigateAboutApp: () -> Unit,
    onNavigateAboutErthi: () -> Unit,
    onLockApp: () -> Unit = {},
    onNavigatePrivacy: () -> Unit = {},
    onNavigateEncryption: () -> Unit = {},
    onNavigateRecoveryKey: () -> Unit = {},
    onNavigateLanguagePicker: () -> Unit = {},
) {
    val biometricsEnabled by authViewModel.isBiometricEnabled().collectAsState(initial = false)
    Scaffold(
        topBar = { ErthiTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
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
                title = stringResource(R.string.settings_lock),
                subtitle = stringResource(R.string.settings_lock_desc),
                onClick = onLockApp,
            )
            AndalusianCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrnamentalIcon(Icons.Default.Fingerprint, null, tint = RoyalGoldLight)
                    Spacer(Modifier.width(12.dp))
                    val label = stringResource(R.string.auth_use_biometrics)
                    Text(label, modifier = Modifier.weight(1f), color = TextPrimary)
                    Switch(checked = biometricsEnabled, onCheckedChange = authViewModel::setBiometricEnabled)
                }
            }
            AndalusianSettingItem(
                icon = Icons.Default.Security,
                iconColor = SuccessGreenLight,
                title = stringResource(R.string.settings_encrypt),
                subtitle = stringResource(R.string.settings_encrypt_desc),
                onClick = onNavigateEncryption,
            )
            AndalusianSettingItem(
                icon = Icons.Default.VpnKey,
                iconColor = PurpleEnamelLight,
                title = stringResource(R.string.settings_recovery_key),
                subtitle = "النسخ الاحتياطي المشفر كمسار للاسترداد",
                badge = "معلومات",
                badgeColor = PurpleEnamelLight,
                onClick = onNavigateRecoveryKey,
            )
            Text(
                stringResource(R.string.settings_death_unavailable),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
            )
            AndalusianSettingItem(
                icon = Icons.Default.VisibilityOff,
                iconColor = DigitalColor,
                title = stringResource(R.string.settings_privacy),
                subtitle = "بياناتك محلية — اعرف كيف تُحفظ",
                onClick = onNavigatePrivacy,
            )
            AndalusianSettingItem(
                icon = Icons.Default.Language,
                iconColor = RoyalGoldLight,
                title = stringResource(R.string.settings_language),
                subtitle = stringResource(R.string.language_picker_title),
                onClick = onNavigateLanguagePicker,
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            AndalusianSettingItem(
                icon = Icons.Default.Info,
                iconColor = MetallicSilver,
                title = stringResource(R.string.about_app_title),
                subtitle = "إرثي أمانة - مشروع خيري مفتوح المصدر",
                onClick = onNavigateAboutApp,
            )
            AndalusianSettingItem(
                icon = Icons.Default.Favorite,
                iconColor = PinkEnamel,
                title = stringResource(R.string.about_erthi_title),
                subtitle = "مبادرة إرثي الخيرية",
                onClick = onNavigateAboutErthi,
            )

            Spacer(modifier = Modifier.weight(1f).defaultMinSize(minHeight = 24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                OrnamentalIcon(Icons.Default.Shield, null, tint = RoyalGold, modifier = Modifier.size(40.dp))
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
