package com.erthi.amanah.ui.screens.will

import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.WillAccessViewModel
import kotlinx.coroutines.launch

@Composable
fun WillAccessScreen(
    onBack: () -> Unit,
    viewModel: WillAccessViewModel = hiltViewModel(),
) {
    val beneficiaries by viewModel.beneficiaries.collectAsState()
    // Local mutable access map: beneficiaryId → canAccessWill
    val accessMap = remember(beneficiaries) {
        mutableStateMapOf<Long, Boolean>().apply {
            beneficiaries.forEach { put(it.id, it.canAccessWill) }
        }
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; pinInput = ""; pinError = null },
            containerColor = ObsidianCard,
            title = { Text("تأكيد الرمز السري", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل الرمز السري لحفظ إعدادات الوصول", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; pinError = null },
                        label = { Text("الرمز السري") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it, color = EmergencyRed) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = MetallicBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = viewModel.verifyPin(pinInput)
                            if (ok) {
                                val updated = beneficiaries.map { b ->
                                    b.copy(canAccessWill = accessMap[b.id] ?: b.canAccessWill)
                                }
                                viewModel.saveAccessPolicies(updated)
                                showPinDialog = false
                                pinInput = ""
                                saveSuccess = true
                            } else {
                                pinError = "رمز سري خاطئ"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    enabled = pinInput.isNotBlank(),
                ) { Text("تأكيد", color = GlossyBlack) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinInput = ""; pinError = null }) {
                    Text("إلغاء", color = MetallicSilver)
                }
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "إعدادات الوصول للوصية", onBack = onBack) },
        containerColor = GlossyBlack,
        bottomBar = {
            Surface(color = GlossyBlackMid, tonalElevation = 0.dp) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    Button(
                        onClick = { saveSuccess = false; showPinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = GlossyBlack),
                        enabled = beneficiaries.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("حفظ إعدادات الوصول", fontWeight = FontWeight.Bold)
                    }
                    if (beneficiaries.isEmpty()) {
                        Text(
                            "أضف مستفيدين أولاً لتفعيل إعدادات الوصول",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D0A00)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "فعّل الوصول للأشخاص الموثوقين الذين تريدهم أن يطّلعوا على وصيتك بعد وفاتك. يتطلب حفظ أي تغيير إدخال الرمز السري.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            if (saveSuccess) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF061A0E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, SuccessGreenLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreenLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("✅ تم حفظ إعدادات الوصول بنجاح", color = SuccessGreenLight, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (beneficiaries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Default.People, null, tint = MetallicSilver, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("لا يوجد مستفيدون مضافون بعد", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text("أضف مستفيدين من شاشة «الأسرة والمستفيدون»", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                item {
                    Text("المستفيدون (${beneficiaries.size})", style = MaterialTheme.typography.labelMedium, color = MetallicSilver)
                }
                items(beneficiaries, key = { it.id }) { beneficiary ->
                    val canAccess = accessMap[beneficiary.id] ?: beneficiary.canAccessWill
                    BeneficiaryAccessCard(
                        beneficiary = beneficiary,
                        canAccess = canAccess,
                        onToggle = { accessMap[beneficiary.id] = !canAccess },
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BeneficiaryAccessCard(
    beneficiary: BeneficiaryEntity,
    canAccess: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(
            1.dp,
            if (canAccess) GoldAccent.copy(alpha = 0.5f) else MetallicBorder,
            RoundedCornerShape(12.dp),
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canAccess) androidx.compose.ui.graphics.Color(0xFF0D0A00) else ObsidianCard,
        ),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                null,
                tint = if (canAccess) GoldAccent else MetallicSilver,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(beneficiary.fullName, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                if (beneficiary.relation.isNotBlank()) {
                    Text(beneficiary.relation, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (beneficiary.phone.isNotBlank()) {
                    Text(beneficiary.phone, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = canAccess,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldAccent,
                        checkedTrackColor = GoldAccent.copy(alpha = 0.3f),
                        uncheckedThumbColor = MetallicSilver,
                        uncheckedTrackColor = MetallicBorder,
                    ),
                )
                Text(
                    if (canAccess) "مصرّح" else "محجوب",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canAccess) GoldAccent else TextMuted,
                )
            }
        }
    }
}
