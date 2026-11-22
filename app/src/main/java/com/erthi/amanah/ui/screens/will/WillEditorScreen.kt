package com.erthi.amanah.ui.screens.will

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.WillViewModel

@Composable
fun WillEditorScreen(
    onBack: () -> Unit,
    viewModel: WillViewModel = hiltViewModel(),
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            containerColor = NavyCard,
            title = { Text("تنبيه قانوني", color = TextPrimary) },
            text = {
                Text(
                    "هذه الوصية الإلكترونية ليست بديلاً عن الوصية القانونية الرسمية. " +
                    "يُنصح بتوثيقها لدى جهة قانونية معتمدة. تأكد من مراجعة أحكام الشريعة الإسلامية " +
                    "وقانون الأسرة المعمول به في بلدك.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLegalDialog = false
                    viewModel.saveWill(title = title, content = content, isDraft = false)
                    saved = true
                    onBack()
                }) { Text("فهمت — حفظ الوصية", color = SilverBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showLegalDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            ErthiTopBar(
                title = "كتابة الوصية",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        viewModel.saveWill(title = title, content = content, isDraft = true)
                        saved = true
                        onBack()
                    }) {
                        OrnamentalIcon(Icons.Default.Save, contentDescription = "حفظ كمسودة", tint = SilverBlue)
                    }
                },
            )
        },
        containerColor = NavyDeep,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الوصية", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = erthiFieldColors(),
                singleLine = true,
                placeholder = { Text("مثال: وصيتي لأبنائي", color = TextMuted) },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("نص الوصية", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = erthiFieldColors(),
                maxLines = Int.MAX_VALUE,
                placeholder = { Text("اكتب وصيتك هنا...", color = TextMuted) },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "تُحفظ الوصية محلياً ومشفرة على جهازك فقط.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showLegalDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SilverBlue, contentColor = NavyDeep),
                enabled = content.isNotBlank(),
            ) {
                Text("حفظ الوصية نهائياً", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun erthiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SilverBlue, unfocusedBorderColor = DividerColor,
    focusedLabelColor = SilverBlue, cursorColor = SilverBlue,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)
