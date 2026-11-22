package com.erthi.amanah.ui.screens.beneficiaries

import com.erthi.amanah.ui.components.OrnamentalIcon
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
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.BeneficiaryViewModel

@Composable
fun BeneficiariesScreen(
    onBack: () -> Unit,
    viewModel: BeneficiaryViewModel = hiltViewModel(),
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val beneficiaries by viewModel.beneficiaries.collectAsState()

    if (showAddDialog) {
        AddBeneficiaryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, relation, phone, email, isEmergency ->
                viewModel.addBeneficiary(name, relation, phone, email, isEmergency)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الأسرة والمستفيدون", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BeneficiariesColor,
                contentColor = NavyDeep,
            ) { OrnamentalIcon(Icons.Default.Add, null) }
        },
    ) { padding ->
        if (beneficiaries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("لا توجد جهات اتصال مسجّلة\nاضغط + لإضافة شخص")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(beneficiaries, key = { it.id }) { b ->
                    BeneficiaryCard(b, onDelete = { viewModel.delete(b) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun BeneficiaryCard(b: BeneficiaryEntity, onDelete: () -> Unit) {
    val isEmergency = b.isEmergencyContact
    val iconColor = if (isEmergency) EmergencyRed else BeneficiariesColor
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                OrnamentalIcon(
                    if (isEmergency) Icons.Default.Warning else Icons.Default.Person,
                    null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(b.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(b.relation, style = MaterialTheme.typography.bodySmall, color = if (isEmergency) EmergencyRed else BeneficiariesColor)
                if (b.phone.isNotBlank()) Text(b.phone, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            IconButton(onClick = onDelete) {
                OrnamentalIcon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddBeneficiaryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isEmergency by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        title = { Text("إضافة شخص", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم الكامل *", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true)
                OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("صلة القرابة", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true, placeholder = { Text("مثال: الابن، الزوجة", color = TextMuted) })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEmergency, onCheckedChange = { isEmergency = it }, colors = CheckboxDefaults.colors(checkedColor = BeneficiariesColor))
                    Text("جهة اتصال للطوارئ", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, relation, phone, email, isEmergency) }, enabled = name.isNotBlank()) {
                Text("إضافة", color = BeneficiariesColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) } },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BeneficiariesColor, unfocusedBorderColor = DividerColor,
    focusedLabelColor = BeneficiariesColor, cursorColor = BeneficiariesColor,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)
