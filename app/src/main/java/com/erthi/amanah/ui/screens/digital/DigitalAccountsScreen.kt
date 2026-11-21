package com.erthi.amanah.ui.screens.digital

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.DigitalAccountEntity
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.DigitalAccountViewModel

@Composable
fun DigitalAccountsScreen(
    onBack: () -> Unit,
    viewModel: DigitalAccountViewModel = hiltViewModel(),
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val accounts by viewModel.accounts.collectAsState()

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { platform, identifier, instructions, notes ->
                viewModel.addAccount(platform, identifier, instructions, notes)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الحسابات الرقمية", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = DigitalColor, contentColor = NavyDeep) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        if (accounts.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("لا توجد حسابات رقمية مسجّلة\nاضغط + لإضافة حساب")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.id }) { account ->
                    AccountCard(account, onDelete = { viewModel.delete(account) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun AccountCard(account: DigitalAccountEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(DigitalColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Devices, null, tint = DigitalColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.platform, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(account.accountIdentifier, style = MaterialTheme.typography.bodySmall, color = DigitalColor)
                if (account.instructions.isNotBlank()) Text(account.instructions, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var platform by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = NavyCard,
        title = { Text("إضافة حساب رقمي", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("المنصة *", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true, placeholder = { Text("مثال: Gmail، WhatsApp، Twitter", color = TextMuted) })
                OutlinedTextField(value = identifier, onValueChange = { identifier = it }, label = { Text("معرّف الحساب *", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true, placeholder = { Text("البريد الإلكتروني أو اسم المستخدم", color = TextMuted) })
                OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("تعليمات للورثة", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), maxLines = 2, placeholder = { Text("ماذا تريد أن يفعل الورثة بهذا الحساب؟", color = TextMuted) })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), maxLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(platform, identifier, instructions, notes) }, enabled = platform.isNotBlank() && identifier.isNotBlank()) { Text("إضافة", color = DigitalColor, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) } },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = DigitalColor, unfocusedBorderColor = DividerColor, focusedLabelColor = DigitalColor, cursorColor = DigitalColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
