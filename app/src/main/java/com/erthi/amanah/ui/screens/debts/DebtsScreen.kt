package com.erthi.amanah.ui.screens.debts

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.DebtDirection
import com.erthi.amanah.data.db.entity.DebtEntity
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.DebtViewModel

@Composable
fun DebtsScreen(
    onBack: () -> Unit,
    viewModel: DebtViewModel = hiltViewModel(),
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val debts by viewModel.debts.collectAsState()
    val owedByMe = debts.filter { it.direction == DebtDirection.OWED_BY_ME }
    val owedToMe = debts.filter { it.direction == DebtDirection.OWED_TO_ME }

    if (showAddDialog) {
        AddDebtDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, description, direction ->
                viewModel.addDebt(name, amount, description, direction)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الديون والمستحقات", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DebtsColor,
                contentColor = NavyDeep,
            ) { OrnamentalIcon(Icons.Default.Add, null) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("ما عليك من ديون") }
            if (owedByMe.isEmpty()) {
                item { EmptyState("لا توجد ديون مسجّلة\nاضغط + لإضافة دين") }
            } else {
                items(owedByMe, key = { it.id }) { debt ->
                    DebtCard(debt, onDelete = { viewModel.deleteDebt(debt) })
                }
            }
            item { SectionHeader("ما لك عند الآخرين") }
            if (owedToMe.isEmpty()) {
                item { EmptyState("لا توجد مبالغ مستحقة\nاضغط + لإضافة مبلغ") }
            } else {
                items(owedToMe, key = { it.id }) { debt ->
                    DebtCard(debt, onDelete = { viewModel.deleteDebt(debt) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun DebtCard(debt: DebtEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = if (debt.direction == DebtDirection.OWED_BY_ME)
                Icons.Default.TrendingDown else Icons.Default.TrendingUp
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(DebtsColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                OrnamentalIcon(icon, null, tint = DebtsColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(debt.personName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${debt.amount} ${debt.currency}", style = MaterialTheme.typography.bodyMedium, color = DebtsColor)
                if (debt.description.isNotBlank()) {
                    Text(debt.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                OrnamentalIcon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, DebtDirection) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(DebtDirection.OWED_BY_ME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        title = { Text("إضافة دين / مستحق", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    FilterChip(
                        selected = direction == DebtDirection.OWED_BY_ME,
                        onClick = { direction = DebtDirection.OWED_BY_ME },
                        label = { Text("عليّ") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DebtsColor.copy(alpha = 0.18f), selectedLabelColor = DebtsColor),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = direction == DebtDirection.OWED_TO_ME,
                        onClick = { direction = DebtDirection.OWED_TO_ME },
                        label = { Text("لي") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DebtsColor.copy(alpha = 0.18f), selectedLabelColor = DebtsColor),
                    )
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("اسم الشخص", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(), singleLine = true,
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("المبلغ (جنيه)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("ملاحظات (اختياري)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(), maxLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, amount.toDoubleOrNull() ?: 0.0, description, direction) },
                enabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text("إضافة", color = DebtsColor, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) }
        },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DebtsColor, unfocusedBorderColor = DividerColor,
    focusedLabelColor = DebtsColor, cursorColor = DebtsColor,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)
