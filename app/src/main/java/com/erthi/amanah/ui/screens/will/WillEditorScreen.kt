package com.erthi.amanah.ui.screens.will

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun WillEditorScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ErthiTopBar(
                title = "كتابة الوصية",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.Save, contentDescription = "حفظ", tint = SilverBlue)
                    }
                }
            )
        },
        containerColor = NavyDeep,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الوصية", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = erthiFieldColors(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("نص الوصية", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = erthiFieldColors(),
                maxLines = Int.MAX_VALUE,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onBack() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SilverBlue, contentColor = NavyDeep),
            ) { Text("حفظ الوصية", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun erthiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SilverBlue, unfocusedBorderColor = DividerColor,
    focusedLabelColor = SilverBlue, cursorColor = SilverBlue,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)
