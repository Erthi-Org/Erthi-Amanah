package com.erthi.amanah.ui.screens.about

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    Scaffold(
        topBar = { ErthiTopBar(title = "عن إرثي", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Default.Shield, null, tint = SilverBlue, modifier = Modifier.size(72.dp))
            Text("إرثي", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("مبادرة خيرية مجانية هادفة إلى مساعدة الأفراد على توثيق شؤونهم ووصاياهم بأمان وخصوصية.", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            AboutSection("مستودع المشروع على GitHub", Icons.Default.Code, "https://github.com/Erthi-Org/Erthi-Amanah")
            AboutSection("الرخصة", Icons.Default.Assignment, "يُرخَّص هذا التطبيق بموجب رخصة مفتوحة المصدر. راجع ملف LICENSE في مستودع المشروع لمزيد من التفاصيل.")
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
