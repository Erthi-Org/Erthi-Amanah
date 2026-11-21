package com.erthi.amanah.ui.screens.feedback

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

private val CATEGORIES = listOf(
    "اقتراح ميزة جديدة",
    "الإبلاغ عن مشكلة",
    "تحسين واجهة المستخدم",
    "ملاحظات عامة",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CATEGORIES[0]) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var consentGiven by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ErthiTopBar(title = "شاركنا اقتراحك", onBack = onBack) },
        containerColor = GlossyBlack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (submitted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✅ تم فتح تطبيق البريد الإلكتروني", fontWeight = FontWeight.Bold, color = SuccessGreenLight)
                        Text("ستجد الرسالة معبأة ومستعدة للإرسال. إذا لم يفتح تطبيق البريد، تحقق من إعدادات الجهاز.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { submitted = false }) {
                            Text("إرسال اقتراح آخر", color = AssetsColor)
                        }
                    }
                }
                return@Column
            }

            // Privacy notice
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.PrivacyTip, null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "لن يُرفق أي ملف حساس تلقائياً. الرسالة تُرسل إلى info@erthi.org عبر تطبيق البريد فقط.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            FeedbackField("الاسم (اختياري)", name, { name = it })
            FeedbackField("البريد الإلكتروني للتواصل (اختياري)", contactEmail, { contactEmail = it })

            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {}, readOnly = true,
                    label = { Text("نوع الاقتراح", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = feedbackFieldColors(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, containerColor = ObsidianElevated) {
                    CATEGORIES.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat, color = TextPrimary) }, onClick = { selectedCategory = cat; categoryExpanded = false })
                    }
                }
            }

            FeedbackField("العنوان *", subject, { subject = it })
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("التفاصيل *", color = TextMuted) },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                colors = feedbackFieldColors(),
                maxLines = 6,
                placeholder = { Text("اكتب اقتراحك أو وصف المشكلة بالتفصيل...", color = TextMuted) },
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = consentGiven,
                    onCheckedChange = { consentGiven = it },
                    colors = CheckboxDefaults.colors(checkedColor = AssetsColor),
                )
                Text(
                    "أوافق على إرسال هذا الاقتراح إلى فريق إرثي",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Button(
                onClick = {
                    val emailBody = buildString {
                        appendLine("نوع الاقتراح: $selectedCategory")
                        if (name.isNotBlank()) appendLine("الاسم: $name")
                        if (contactEmail.isNotBlank()) appendLine("البريد: $contactEmail")
                        appendLine()
                        appendLine(description)
                    }
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:info@erthi.org")
                        putExtra(Intent.EXTRA_SUBJECT, "[$selectedCategory] ${subject.ifBlank { "اقتراح" }}")
                        putExtra(Intent.EXTRA_TEXT, emailBody)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        submitted = true
                    } else {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "إلى: info@erthi.org\n$emailBody")
                        }
                        context.startActivity(Intent.createChooser(share, "إرسال الاقتراح"))
                        submitted = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AssetsColor, contentColor = GlossyBlack),
                enabled = subject.isNotBlank() && description.isNotBlank() && consentGiven,
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(8.dp))
                Text("إرسال الاقتراح", fontWeight = FontWeight.Bold)
            }

            Text(
                "الإرسال عبر تطبيق البريد فقط. لا تُرفق بيانات الوصية أو كلمات المرور تلقائياً.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun FeedbackField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted) },
        modifier = Modifier.fillMaxWidth(),
        colors = feedbackFieldColors(),
        singleLine = true,
    )
}

@Composable
private fun feedbackFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AssetsColor, unfocusedBorderColor = MetallicBorder,
    focusedLabelColor = AssetsColor, cursorColor = AssetsColor,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)
