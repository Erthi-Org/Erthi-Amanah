package com.erthi.amanah.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "معلومات عن التطبيق", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AboutSection("هدف التطبيق", Icons.Default.Info, "إرثي أمانة هو تطبيق خيري مجاني يساعد الأفراد على توثيق وصاياهم والتزاماتهم المالية وجهات اتصالهم لطوارئ، لحماية أسرهم.")
            AboutSection("التخزين المحلي", Icons.Default.PhoneAndroid, "جميع بياناتك تُخزّن محلياً على هاتفك فقط. لا يرسل التطبيق أي بيانات خاصة إلى أي خادم.")
            AboutSection("التشفير", Icons.Default.Security, "يستخدم التطبيق تشفير AES-256 مع Android Keystore لحماية بياناتك من الوصول غير المصرح به.")
            AboutSection("النسخ الاحتياطي المشفّر", Icons.Default.CloudUpload, "يمكنك حفظ نسخة احتياطية مشفرة اختيارياً في حسابك السحابي الشخصي (Google Drive، OneDrive، Dropbox).")
            AboutSection("مركز الطوارئ", Icons.Default.Warning, "توفر ميزة الطوارئ وصولاً سريعاً لأرقام الإسعاف والشرطة والمطافئ وجهات الاتصال الشخصية.")
            AboutSection("المستفيدون", Icons.Default.People, "يمكنك تحديد الأشخاص الموثوقين الذين يحق لهم الاطلاع على معلوماتك في الحالات المحددة.")
            AboutSection("المصدر المفتوح", Icons.Default.Code, "التطبيق مفتوح المصدر ومتاح على GitHub:\nhttps://github.com/Erthi-Org/Erthi-Amanah")
            AboutSection("القيود القانونية", Icons.Default.Gavel, "الوصية المكتوبة في التطبيق هي توثيق شخصي. إلزاميتها القانونية تعتمد على تشريعات بلدك وشروط الإشهاد الرسمي. استشر محامياً للتأكد من صحتها.")
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
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Logo area — gold emblem representation
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D0A00)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().border(1.5.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Circular gold emblem placeholder
                        Box(
                            modifier = Modifier.size(100.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color(0xFF8B6914),
                                            androidx.compose.ui.graphics.Color(0xFFD9AA54),
                                            androidx.compose.ui.graphics.Color(0xFF6B4E0A),
                                        )
                                    )
                                )
                                .border(2.dp, GoldBright, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AutoStories, null, tint = GlossyBlack, modifier = Modifier.size(32.dp))
                                Text("إرثي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GlossyBlack)
                            }
                        }
                        Text("إرثي — Erthi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GoldAccent)
                        Text("«علم نشاركه وأثر نتركه»", style = MaterialTheme.typography.bodyMedium, color = GoldBright, fontWeight = FontWeight.Medium)
                        Text("مشاريع إسلامية مفتوحة المصدر", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("لعلم باق وأجر دائم", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = ObsidianCard), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ما هو إرثي؟", fontWeight = FontWeight.Bold, color = GoldAccent, style = MaterialTheme.typography.titleSmall)
                        Text("إرثي مبادرة إسلامية خيرية غير ربحية هادفة إلى مساعدة الأفراد على توثيق وصاياهم ومهام شؤونهم بأمان وخصوصية، في إطار يحترم أحكام الشريعة الإسلامية.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("وصيتي — أحد مشروعات إرثي الخيرية", fontWeight = FontWeight.Bold, color = MetallicSilver, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Links
            item { SectionHeaderGold("روابط إرثي الرسمية") }
            item { LinkCard("الموقع الرسمي", "https://erthi.org", Icons.Default.Language, GoldAccent) { openUrl("https://erthi.org") } }
            item { LinkCard("البريد الإلكتروني", "info@erthi.org", Icons.Default.Email, DigitalColor) { openEmail() } }
            item { LinkCard("فيسبوك", "صفحة إرثي على فيسبوك", Icons.Default.People, BeneficiariesColor) { openUrl("https://www.facebook.com/share/1FdLqTnPmg/") } }
            item { LinkCard("GitHub", "Erthi-Org / Erthi-Amanah", Icons.Default.Code, MetallicSilver) { openUrl("https://github.com/Erthi-Org/Erthi-Amanah") } }

            item { SectionHeaderGold("معلومات التطبيق") }
            item { AboutSection("الإصدار", Icons.Default.Info, "1.0.0 (Wasiyyati — وصيتي)") }
            item { AboutSection("المصدر المفتوح", Icons.Default.Code, "التطبيق مفتوح المصدر متاح على GitHub للجميع.") }
            item { AboutSection("التشفير", Icons.Default.Security, "AES-256-GCM عبر Android Keystore. بياناتك على هاتفك فقط.") }
            item { AboutSection("القيود القانونية", Icons.Default.Gavel, "الوصية المكتوبة هنا توثيق شخصي. تأكد من إشهادها وتوثيقها لدى جهة قانونية معتمدة.") }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D0A00)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("﴿ وَالْآخِرَةُ خَيْرٌ وَأَبْقَىٰ ﴾", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldAccent)
                        Text("الأعلى: 17", style = MaterialTheme.typography.labelSmall, color = GoldAccent.copy(alpha = 0.7f))
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeaderGold(title: String) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = GoldAccent, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
private fun LinkCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(12.dp)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Default.OpenInNew, null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun AboutSection(title: String, icon: ImageVector, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = SilverBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
