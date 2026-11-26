package com.erthi.amanah.ui.screens.about

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erthi.amanah.BuildConfig
import com.erthi.amanah.R
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "معلومات عن التطبيق", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { AboutSection("هدف التطبيق", Icons.Default.Info, "وصيتي تطبيق خيري مجاني يساعد الأفراد على توثيق وصاياهم والتزاماتهم المالية وجهات اتصالهم لطوارئ، لحماية أسرهم.") }
            item { AboutSection("التخزين المحلي", Icons.Default.PhoneAndroid, "جميع بياناتك تُخزّن محلياً على هاتفك فقط. لا يرسل التطبيق أي بيانات خاصة إلى أي خادم.") }
            item { AboutSection("التشفير", Icons.Default.Security, "يستخدم التطبيق تشفير AES-256 مع Android Keystore لحماية بياناتك من الوصول غير المصرح به.") }
            item { AboutSection("النسخ الاحتياطي المشفّر", Icons.Default.CloudUpload, "يمكنك حفظ نسخة احتياطية مشفرة اختيارياً في حسابك السحابي الشخصي (Google Drive، OneDrive، Dropbox).") }
            item { AboutSection("مركز الطوارئ", Icons.Default.Warning, "توفر ميزة الطوارئ وصولاً سريعاً لأرقام الإسعاف والشرطة والمطافئ وجهات الاتصال الشخصية.") }
            item { AboutSection("المستفيدون", Icons.Default.People, "يمكنك تحديد الأشخاص الموثوقين الذين يحق لهم الاطلاع على معلوماتك في الحالات المحددة.") }
            item { AboutSection("المصدر المفتوح", Icons.Default.Code, "التطبيق مفتوح المصدر ومتاح على GitHub:\nhttps://github.com/Erthi-Org/Erthi-Amanah") }
            item { AboutSection("القيود القانونية", Icons.Default.Gavel, "الوصية المكتوبة في التطبيق هي توثيق شخصي. إلزاميتها القانونية تعتمد على تشريعات بلدك وشروط الإشهاد الرسمي. استشر محامياً للتأكد من صحتها.") }
        }
    }
}

@Composable
fun AboutErthiScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    fun openUrl(url: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    }
    fun openEmail() {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:info@erthi.org")
        }
        if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "عن إرثي", onBack = onBack) },
        containerColor = GlossyBlack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Compact branded header with official Erthi seal
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D0A00)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Official Erthi emblem — ic_erthi_seal.webp (authentic signed asset)
                        // Icon with Color.Unspecified preserves the full color of the WebP drawable
                        Icon(
                            painter = painterResource(id = R.drawable.ic_erthi_seal),
                            contentDescription = "الختم الرسمي لإرثي",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GoldAccent.copy(alpha = 0.5f), CircleShape),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "إرثي — Erthi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                            )
                            Text(
                                "«علم نشاركه وأثر نتركه»",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldBright,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "مشاريع إسلامية مفتوحة المصدر",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ما هو إرثي؟", fontWeight = FontWeight.Bold, color = GoldAccent, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "إرثي مبادرة إسلامية خيرية غير ربحية هادفة إلى مساعدة الأفراد على توثيق وصاياهم ومهام شؤونهم بأمان وخصوصية، في إطار يحترم أحكام الشريعة الإسلامية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Text(
                            "وصيتي — أحد مشروعات إرثي الخيرية",
                            fontWeight = FontWeight.Bold,
                            color = MetallicSilver,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item { SectionHeaderGold("روابط إرثي الرسمية") }
            item { LinkCard("الموقع الرسمي", "https://erthi.org", Icons.Default.Language, GoldAccent) { openUrl("https://erthi.org") } }
            item { LinkCard("البريد الإلكتروني", "info@erthi.org", Icons.Default.Email, DigitalColor) { openEmail() } }
            item { LinkCard("فيسبوك", "صفحة إرثي على فيسبوك", Icons.Default.People, BeneficiariesColor) { openUrl("https://www.facebook.com/share/1FdLqTnPmg/") } }
            item { LinkCard("GitHub", "Erthi-Org / Erthi-Amanah", Icons.Default.Code, MetallicSilver) { openUrl("https://github.com/Erthi-Org/Erthi-Amanah") } }

            item { SectionHeaderGold("معلومات التطبيق") }
            item {
                AboutSection(
                    "الإصدار",
                    Icons.Default.Info,
                    "وصيتي ${BuildConfig.VERSION_NAME} (versionCode: ${BuildConfig.VERSION_CODE})",
                )
            }
            item { AboutSection("المصدر المفتوح", Icons.Default.Code, "التطبيق مفتوح المصدر متاح على GitHub للجميع.") }
            item { AboutSection("التشفير", Icons.Default.Security, "AES-256-GCM عبر Android Keystore. بياناتك على هاتفك فقط.") }
            item { AboutSection("القيود القانونية", Icons.Default.Gavel, "الوصية المكتوبة هنا توثيق شخصي. تأكد من إشهادها وتوثيقها لدى جهة قانونية معتمدة.") }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D0A00)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "﴿ وَالْآخِرَةُ خَيْرٌ وَأَبْقَىٰ ﴾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                        )
                        Text(
                            "الأعلى: 17",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SectionHeaderGold(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = GoldAccent,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun LinkCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MetallicBorder, RoundedCornerShape(12.dp)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            OrnamentalIcon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            OrnamentalIcon(Icons.Default.OpenInNew, null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun AboutSection(title: String, icon: ImageVector, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            OrnamentalIcon(icon, null, tint = SilverBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
