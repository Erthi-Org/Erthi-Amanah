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
import com.erthi.amanah.ui.components.AndalusianCard
import com.erthi.amanah.ui.components.AndalusianIcon
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun EncryptionInfoScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "تشفير البيانات", onBack = onBack) },
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
            // Status banner — encryption is active
            AndalusianCard(accentColor = SuccessGreenLight) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AndalusianIcon(
                        icon = Icons.Default.CheckCircle,
                        iconColor = SuccessGreenLight,
                        size = 44.dp,
                        iconSize = 22.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "التشفير مُفعَّل",
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreenLight,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "جميع ملفاتك مشفرة تلقائياً على هاتفك",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            EncryptionItem(
                title = "التشفير المحلي (ملفات الصوت والفيديو والوثائق)",
                detail = "AES-256-GCM عبر Android Keystore. المفتاح لا يغادر الجهاز أبداً. كل ملف مشفر بـ IV مستقل — لا يؤدي اختراق ملف واحد إلى اختراق بقية الملفات.",
            )
            EncryptionItem(
                title = "تشفير النسخ الاحتياطية الاختيارية",
                detail = "PBKDF2WithHmacSHA256 (310,000 تكرار) مع AES-256-GCM. المفتاح مشتق من كلمة مرور تختارها أنت عند التصدير. إرثي لا تعرف كلمة المرور.",
            )
            EncryptionItem(
                title = "حماية الرمز السري",
                detail = "الرمز السري لا يُحفظ نصاً صريحاً. يُحفظ فقط ناتج PBKDF2-HMAC-SHA256 مع ملح عشوائي (32 بايت) عبر Android DataStore.",
            )
            EncryptionItem(
                title = "قاعدة البيانات (Room)",
                detail = "قاعدة البيانات في وحدة التخزين الداخلية المحمية بنظام التشغيل. الوصول مقيد بالتطبيق فقط. التشفير الكامل لقاعدة البيانات (SQLCipher) غير مفعَّل في الإصدار الحالي — يُجرى دراسته.",
            )
            EncryptionItem(
                title = "ما لا يمكن تعطيله",
                detail = "تشفير الملفات الحساسة مدمج في بنية التطبيق ولا يمكن للمستخدم تعطيله. هذا عمد لحمايتك من إعادة المحاولة عبر الوصول المادي للجهاز.",
            )
        }
    }
}

@Composable
private fun EncryptionItem(title: String, detail: String) {
    AndalusianCard(accentColor = RoyalGold) {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = RoyalGoldLight, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
