package com.erthi.amanah.ui.screens.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.VaultCategory
import com.erthi.amanah.data.db.entity.VaultItemEntity
import com.erthi.amanah.ui.components.EmptyState
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.util.PasswordStrength
import com.erthi.amanah.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordVaultScreen(
    onBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val allItems by viewModel.items.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<VaultItemEntity?>(null) }

    val filtered = remember(allItems, searchQuery, selectedCategory) {
        allItems
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter { searchQuery.isBlank() || it.serviceName.contains(searchQuery, ignoreCase = true) }
    }

    if (showAddDialog || editingItem != null) {
        VaultItemDialog(
            item = editingItem,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false; editingItem = null },
        )
    }

    Scaffold(
        topBar = {
            ErthiTopBar(
                title = "خزنة كلمات المرور",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = VaultColor)
                    }
                },
            )
        },
        containerColor = GlossyBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VaultColor,
                contentColor = GlossyBlack,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header gradient
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF001E2B), GlossyBlack)))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(VaultColor.copy(alpha = 0.15f))
                            .border(1.dp, VaultColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Lock, null, tint = VaultColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${allItems.size} كلمة مرور محفوظة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("مشفّرة بـ AES-256-GCM", style = MaterialTheme.typography.labelSmall, color = VaultColor)
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultColor, unfocusedBorderColor = MetallicBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    cursorColor = VaultColor,
                ),
                singleLine = true,
            )

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CategoryChip("الكل", selectedCategory == null) { selectedCategory = null }
                }
                items(VaultCategory.entries) { cat ->
                    CategoryChip(cat.arabicLabel(), selectedCategory == cat) { selectedCategory = cat }
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(if (searchQuery.isNotBlank()) "لا نتائج للبحث" else "لا توجد كلمات مرور محفوظة\nاضغط + لإضافة بيانات")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { item ->
                        VaultCard(
                            item = item,
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.deleteItem(item) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VaultColor.copy(alpha = 0.2f),
            selectedLabelColor = VaultColor,
            containerColor = ObsidianCard,
            labelColor = TextSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = MetallicBorder, selectedBorderColor = VaultColor,
        ),
    )
}

@Composable
private fun VaultCard(item: VaultItemEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = ObsidianCard,
            title = { Text("حذف البيانات", color = TextPrimary) },
            text = { Text("سيتم حذف بيانات «${item.serviceName}» نهائياً.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("حذف", color = EmergencyRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء", color = TextSecondary) }
            },
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(item.category.color().copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.category.icon(), null, tint = item.category.color(), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.serviceName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.category.arabicLabel(), style = MaterialTheme.typography.labelSmall, color = item.category.color())
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = EmergencyRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
            if (item.username != null || item.email != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    item.username ?: item.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (passwordVisible) item.password else "••••••••••••",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (passwordVisible) MetallicSilver else TextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = VaultColor, modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultItemDialog(
    item: VaultItemEntity?,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit,
) {
    var serviceName by remember { mutableStateOf(item?.serviceName ?: "") }
    var category by remember { mutableStateOf(item?.category ?: VaultCategory.OTHER) }
    var websiteUrl by remember { mutableStateOf(item?.websiteUrl ?: "") }
    var username by remember { mutableStateOf(item?.username ?: "") }
    var email by remember { mutableStateOf(item?.email ?: "") }
    var password by remember { mutableStateOf(item?.password ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val strength = remember(password) { viewModel.passwordStrength(password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                if (item == null) "إضافة كلمة مرور" else "تعديل البيانات",
                color = TextPrimary, fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VaultField("اسم الخدمة *", serviceName, { serviceName = it })

                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = category.arabicLabel(),
                        onValueChange = {}, readOnly = true,
                        label = { Text("الفئة", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = vaultFieldColors(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, containerColor = ObsidianElevated) {
                        VaultCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.arabicLabel(), color = TextPrimary) },
                                onClick = { category = cat; categoryExpanded = false },
                            )
                        }
                    }
                }

                VaultField("الموقع الإلكتروني", websiteUrl, { websiteUrl = it })
                VaultField("اسم المستخدم", username, { username = it })
                VaultField("البريد الإلكتروني", email, { email = it }, KeyboardType.Email)

                // Password field with strength indicator
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور *", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = vaultFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { password = viewModel.generatePassword() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.AutoFixHigh, null, tint = VaultColor, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(32.dp)) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                )
                if (password.isNotEmpty()) {
                    PasswordStrengthBar(strength)
                }

                VaultField("ملاحظات", notes, { notes = it }, maxLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.saveItem(
                        id = item?.id ?: 0,
                        serviceName = serviceName, category = category,
                        websiteUrl = websiteUrl, username = username, email = email,
                        password = password, accountIdentifier = null,
                        recoveryEmail = null, recoveryPhone = null,
                        recoveryCodes = null, notes = notes,
                    )
                    onDismiss()
                },
                enabled = serviceName.isNotBlank() && password.isNotBlank(),
            ) {
                Text("حفظ", color = VaultColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) }
        },
    )
}

@Composable
private fun PasswordStrengthBar(strength: PasswordStrength) {
    val (color, label) = when (strength) {
        PasswordStrength.WEAK -> EmergencyRed to "ضعيفة"
        PasswordStrength.MEDIUM -> WarningAmber to "متوسطة"
        PasswordStrength.STRONG -> SuccessGreenLight to "قوية"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val filled = when (strength) {
                PasswordStrength.WEAK -> i < 1
                PasswordStrength.MEDIUM -> i < 2
                PasswordStrength.STRONG -> true
            }
            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (filled) color else MetallicBorder))
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun VaultField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        colors = vaultFieldColors(),
        textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        maxLines = maxLines,
        singleLine = maxLines == 1,
    )
}

@Composable
private fun vaultFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VaultColor, unfocusedBorderColor = MetallicBorder,
    focusedLabelColor = VaultColor, cursorColor = VaultColor,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)

private fun VaultCategory.arabicLabel() = when (this) {
    VaultCategory.BANK -> "بنك وحساب مالي"
    VaultCategory.SOCIAL_MEDIA -> "شبكة اجتماعية"
    VaultCategory.EMAIL -> "بريد إلكتروني"
    VaultCategory.WEBSITE -> "موقع إلكتروني"
    VaultCategory.APP -> "تطبيق"
    VaultCategory.OTHER -> "أخرى"
}

private fun VaultCategory.icon(): ImageVector = when (this) {
    VaultCategory.BANK -> Icons.Default.AccountBalance
    VaultCategory.SOCIAL_MEDIA -> Icons.Default.People
    VaultCategory.EMAIL -> Icons.Default.Email
    VaultCategory.WEBSITE -> Icons.Default.Language
    VaultCategory.APP -> Icons.Default.Apps
    VaultCategory.OTHER -> Icons.Default.Key
}

private fun VaultCategory.color(): Color = when (this) {
    VaultCategory.BANK -> FinanceColor
    VaultCategory.SOCIAL_MEDIA -> BeneficiariesColor
    VaultCategory.EMAIL -> DigitalColor
    VaultCategory.WEBSITE -> VaultColor
    VaultCategory.APP -> AssetsColor
    VaultCategory.OTHER -> MetallicSilver
}
