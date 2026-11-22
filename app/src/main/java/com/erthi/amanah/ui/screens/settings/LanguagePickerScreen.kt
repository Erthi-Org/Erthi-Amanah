package com.erthi.amanah.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

private data class LanguageEntry(
    val tag: String,
    val flag: String,
    val nativeName: String,
    val displayName: String,
)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageEntry("ar",    "🇸🇦", "العربية",        "Arabic"),
    LanguageEntry("en",    "🇬🇧", "English",         "English"),
    LanguageEntry("fr",    "🇫🇷", "Français",        "French"),
    LanguageEntry("de",    "🇩🇪", "Deutsch",         "German"),
    LanguageEntry("es",    "🇪🇸", "Español",         "Spanish"),
    LanguageEntry("it",    "🇮🇹", "Italiano",        "Italian"),
    LanguageEntry("hi",    "🇮🇳", "हिन्दी",          "Hindi"),
    LanguageEntry("ur",    "🇵🇰", "اردو",            "Urdu"),
    LanguageEntry("fa",    "🇮🇷", "فارسی",           "Persian"),
    LanguageEntry("ms",    "🇲🇾", "Bahasa Melayu",   "Malay"),
    LanguageEntry("fil",   "🇵🇭", "Filipino",        "Filipino"),
)

@Composable
fun LanguagePickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentLocale = remember {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .split(",").firstOrNull()?.trim()?.split("-")?.firstOrNull() ?: "ar"
    }
    var selectedTag by remember { mutableStateOf(currentLocale) }

    Scaffold(
        topBar = { ErthiTopBar(title = "اختيار اللغة", onBack = onBack) },
        containerColor = AndalusianBlue,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                // Ornamental header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "— اختر لغة التطبيق —",
                        color = RoyalGoldDim,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            items(SUPPORTED_LANGUAGES) { lang ->
                LanguageRow(
                    entry = lang,
                    isSelected = selectedTag == lang.tag,
                    onSelect = {
                        selectedTag = lang.tag
                        val localeList = LocaleListCompat.forLanguageTags(lang.tag)
                        AppCompatDelegate.setApplicationLocales(localeList)
                    },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                // Disclaimer about provisional translations
                Text(
                    text = "* الترجمات مؤقتة وقد تحتاج إلى مراجعة",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    entry: LanguageEntry,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val borderColor = if (isSelected) RoyalGold else AndalusianBorder
    val bgColor = if (isSelected) AndalusianElevated else AndalusianCard

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flag emoji
        Text(
            text = entry.flag,
            fontSize = 24.sp,
            modifier = Modifier.size(32.dp),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.nativeName,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) RoyalGoldLight else TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = entry.displayName,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
