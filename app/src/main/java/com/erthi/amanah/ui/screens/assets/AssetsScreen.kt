package com.erthi.amanah.ui.screens.assets

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.erthi.amanah.data.db.entity.AssetEntity
import com.erthi.amanah.data.db.entity.AssetType
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.AssetViewModel

@Composable
fun AssetsScreen(
    onBack: () -> Unit,
    viewModel: AssetViewModel = hiltViewModel(),
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val assets by viewModel.assets.collectAsState()

    if (showAddDialog) {
        AddAssetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, description, value, location ->
                viewModel.addAsset(name, type, description, value, location)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الممتلكات", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = AssetsColor, contentColor = NavyDeep) {
                OrnamentalIcon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        if (assets.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyState("لا توجد ممتلكات مسجّلة\nاضغط + لإضافة ممتلك")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(assets, key = { it.id }) { asset ->
                    AssetCard(asset, onDelete = { viewModel.delete(asset) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun AssetCard(asset: AssetEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(AssetsColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                OrnamentalIcon(assetTypeIcon(asset.type), null, tint = AssetsColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(asset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(assetTypeLabel(asset.type), style = MaterialTheme.typography.labelSmall, color = AssetsColor)
                if (asset.estimatedValue != null) Text("${asset.estimatedValue} ${asset.currency}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = onDelete) { OrnamentalIcon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp)) }
        }
    }
}

private fun assetTypeLabel(type: AssetType) = when (type) {
    AssetType.BANK_ACCOUNT -> "حساب بنكي"
    AssetType.REAL_ESTATE -> "عقار"
    AssetType.VEHICLE -> "سيارة"
    AssetType.GOLD -> "ذهب"
    AssetType.BUSINESS -> "نشاط تجاري"
    AssetType.INVESTMENT -> "استثمار"
    AssetType.OTHER -> "أخرى"
}

private fun assetTypeIcon(type: AssetType) = when (type) {
    AssetType.BANK_ACCOUNT -> Icons.Default.AccountBalance
    AssetType.REAL_ESTATE -> Icons.Default.HomeWork
    AssetType.VEHICLE -> Icons.Default.DirectionsCar
    AssetType.GOLD -> Icons.Default.Stars
    AssetType.BUSINESS -> Icons.Default.Business
    AssetType.INVESTMENT -> Icons.Default.TrendingUp
    AssetType.OTHER -> Icons.Default.Inventory
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAssetDialog(onDismiss: () -> Unit, onConfirm: (String, AssetType, String, Double?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AssetType.OTHER) }
    var description by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = NavyCard,
        title = { Text("إضافة ممتلك", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم *", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = assetTypeLabel(type), onValueChange = {}, readOnly = true, label = { Text("النوع", color = TextSecondary) }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = dialogFieldColors(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AssetType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(assetTypeLabel(t)) }, onClick = { type = t; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("القيمة التقديرية (جنيه)", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("الموقع / المكان", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("ملاحظات", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), maxLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, type, description, value.toDoubleOrNull(), location) }, enabled = name.isNotBlank()) { Text("إضافة", color = AssetsColor, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) } },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = AssetsColor, unfocusedBorderColor = DividerColor, focusedLabelColor = AssetsColor, cursorColor = AssetsColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
