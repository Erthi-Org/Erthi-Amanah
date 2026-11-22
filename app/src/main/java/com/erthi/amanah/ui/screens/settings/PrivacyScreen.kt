package com.erthi.amanah.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.AndalusianCard
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "الخصوصية", onBack = onBack) },
        containerColor = AndalusianBlue,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PrivacyItem(
                icon = Icons.Default.PhoneAndroid,
                iconColor = SuccessGreenLight,
                title = "التخزين المحلي",
                body = "جميع بياناتك — الوصية، الأموال، الديون، الممتلكات، الوثائق والمستفيدون — محفوظة حصراً على هاتفك في وحدة التخزين الداخلية المحمية بنظام التشغيل. لا توجد قاعدة بيانات خارجية تخص إرثي.",
            )
            PrivacyItem(
                icon = Icons.Default.Lock,
                iconColor = RoyalGold,
                title = "التشفير المحلي",
                body = "كل الملفات الحساسة (الصوت والفيديو والوثائق) مشفرة بـ AES-256-GCM باستخدام Android Keystore. لا تُرسَل مفاتيح التشفير إلى أي خارج. الرمز السري مُجزَّأ بـ SHA-256 مع ملح عشوائي ومحفوظ في DataStore.",
            )
            PrivacyItem(
                icon = Icons.Default.CloudUpload,
                iconColor = DigitalColor,
                title = "النسخ الاحتياطي السحابي (اختياري)",
                body = "يمكنك بشكل اختياري نسخ بياناتك احتياطياً إلى Google Drive الخاص بك. تُشفَّر النسخة الاحتياطية بكلمة مرور تختارها أنت قبل الرفع. لا يمتلك فريق إرثي أو Google إمكانية الوصول إلى مفتاح التشفير. إرثي لا تعمِّل خوادمها السحابية الخاصة لتخزين بياناتك.",
            )
            PrivacyItem(
                icon = Icons.Default.Wifi,
                iconColor = AssetsColor,
                title = "يعمل دون إنترنت",
                body = "التطبيق كامل الوظائف بدون اتصال بالإنترنت. الإنترنت مطلوب فقط لاختياري Google Drive.",
            )
            PrivacyItem(
                icon = Icons.Default.VisibilityOff,
                iconColor = PurpleEnamelLight,
                title = "لا تتبع ولا إعلانات",
                body = "لا توجد تحليلات أو تتبع سلوك أو أدوات إعلانية في التطبيق. المشروع خيري مفتوح المصدر.",
            )
            PrivacyItem(
                icon = Icons.Default.Description,
                iconColor = WillColor,
                title = "الوصول إلى الوصية",
                body = "إمكانية الوصول بعد الوفاة تعتمد على آلية تختارها أنت (تبليغ المستفيدين بالرمز السري مسبقاً، أو النسخة الاحتياطية المشفرة). لا توجد آلية تحقق تلقائي من الوفاة عبر خادم خارجي.",
            )
        }
    }
}

@Composable
private fun PrivacyItem(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: androidx.compose.ui.graphics.Color, title: String, body: String) {
    AndalusianCard(accentColor = iconColor) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
