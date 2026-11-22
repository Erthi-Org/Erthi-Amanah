package com.erthi.amanah.ui.screens.settings

import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.erthi.amanah.R
import com.erthi.amanah.ui.components.AndalusianIcon
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

private data class LanguageEntry(val tag: String, @RawRes val flag: Int, val nativeName: String)

private val languages = listOf(
    LanguageEntry("ar", R.raw.flag_sa, "العربية"),
    LanguageEntry("en", R.raw.flag_gb, "English"),
    LanguageEntry("fr", R.raw.flag_fr, "Français"),
    LanguageEntry("de", R.raw.flag_de, "Deutsch"),
    LanguageEntry("es", R.raw.flag_es, "Español"),
    LanguageEntry("it", R.raw.flag_it, "Italiano"),
    LanguageEntry("hi", R.raw.flag_in, "हिन्दी"),
    LanguageEntry("ur", R.raw.flag_pk, "اردو"),
    LanguageEntry("fa", R.raw.flag_ir, "فارسی"),
    LanguageEntry("ms", R.raw.flag_my, "Bahasa Melayu"),
    LanguageEntry("fil", R.raw.flag_ph, "Filipino"),
)

@Composable
fun LanguagePickerScreen(onBack: () -> Unit) {
    val configuredTag = LocalConfiguration.current.locales[0].language
    val selectedTag = AppCompatDelegate.getApplicationLocales()[0]?.language
        ?: languages.firstOrNull { it.tag == configuredTag }?.tag ?: "ar"

    Scaffold(
        topBar = { ErthiTopBar(title = stringResource(R.string.language_picker_title), onBack = onBack) },
        containerColor = AndalusianBlue,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(languages, key = { it.tag }) { language ->
                LanguageRow(language, selectedTag == language.tag) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
                }
            }
            item {
                Text(
                    text = stringResource(R.string.language_provisional),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(entry: LanguageEntry, isSelected: Boolean, onSelect: () -> Unit) {
    val context = LocalContext.current
    val flag = remember(context, entry.flag) {
        ImageRequest.Builder(context).data(entry.flag).decoderFactory(SvgDecoder.Factory()).build()
    }
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) AndalusianElevated else AndalusianCard)
            .border(1.dp, if (isSelected) RoyalGold else AndalusianBorder, shape)
            .selectable(isSelected, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = flag,
            contentDescription = null,
            modifier = Modifier.size(36.dp, 27.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, RoyalGold.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            entry.nativeName,
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily.Default,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) RoyalGoldLight else TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (isSelected) AndalusianIcon(Icons.Default.Check, RoyalGold, size = 28.dp, iconSize = 16.dp)
    }
}
