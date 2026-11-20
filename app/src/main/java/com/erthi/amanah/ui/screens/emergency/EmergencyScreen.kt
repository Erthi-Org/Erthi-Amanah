package com.erthi.amanah.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.EmergencyViewModel

data class EmergencyNumber(val number: String, val label: String, val icon: ImageVector)

@Composable
fun EmergencyScreen(
    onBack: () -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val emergencyContacts by viewModel.emergencyContacts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<BeneficiaryEntity?>(null) }

    val numbers = listOf(
        EmergencyNumber("123", "الإسعاف", Icons.Default.LocalHospital),
        EmergencyNumber("122", "الشرطة", Icons.Default.LocalPolice),
        EmergencyNumber("180", "المطافئ", Icons.Default.LocalFireDepartment),
        EmergencyNumber("112", "طوارئ عامة", Icons.Default.Warning),
    )

    fun dial(number: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    fun sendSos() {
        val phones = emergencyContacts.map { it.phone }.filter { it.isNotBlank() }
        val body = "🆘 طارئ — أحتاج مساعدة عاجلة. يُرجى الاتصال بي فوراً."
        if (phones.isNotEmpty()) {
            // Send to all contacts one by one via SMS chooser
            val firstPhone = phones.first()
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$firstPhone")
                putExtra("sms_body", body)
            }
            if (smsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(smsIntent)
            } else {
                context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) },
                    "إرسال استغاثة",
                ))
            }
        } else {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                putExtra("sms_body", body)
            }
            if (smsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(smsIntent)
            } else {
                context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) },
                    "إرسال استغاثة",
                ))
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, priority ->
                viewModel.addContact(name, phone, priority)
                showAddDialog = false
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = ObsidianCard,
            title = { Text("حذف جهة الاتصال", color = TextPrimary) },
            text = { Text("هل تريد حذف «${target.fullName}» من جهات الطوارئ؟", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteContact(target); deleteTarget = null }) {
                    Text("حذف", color = EmergencyRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("إلغاء", color = MetallicSilver) }
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "مركز الطوارئ", onBack = onBack) },
        containerColor = GlossyBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmergencyRed,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة جهة طوارئ")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF3A0808))
                        .border(1.dp, EmergencyRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = EmergencyRed, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("اتصل بسرعة في الحالات الطارئة", color = EmergencyRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text("أرقام الطوارئ — مصر", style = MaterialTheme.typography.labelMedium, color = MetallicSilver, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    numbers.take(2).forEach { n ->
                        EmergencyNumberCard(n, modifier = Modifier.weight(1f), onClick = { dial(n.number) })
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    numbers.drop(2).forEach { n ->
                        EmergencyNumberCard(n, modifier = Modifier.weight(1f), onClick = { dial(n.number) })
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "جهات الاتصال الشخصية (${emergencyContacts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MetallicSilver,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (emergencyContacts.isEmpty()) {
                item {
                    Card(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, null, tint = MetallicSilver)
                            Spacer(Modifier.width(10.dp))
                            Text("اضغط لإضافة جهة اتصال للطوارئ", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(emergencyContacts, key = { it.id }) { contact ->
                    EmergencyContactCard(contact = contact, onDelete = { deleteTarget = contact })
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item {
                Button(
                    onClick = { sendSos() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                ) {
                    Icon(Icons.Default.Sos, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (emergencyContacts.isEmpty()) "إرسال استغاثة (اختر مستلماً)"
                        else "إرسال استغاثة إلى ${emergencyContacts.first().fullName}",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun EmergencyNumberCard(n: EmergencyNumber, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.border(1.dp, EmergencyRed.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0808)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(n.icon, null, tint = EmergencyRed, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(n.number, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
            Text(n.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmergencyContactCard(contact: BeneficiaryEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(EmergencyRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Person, null, tint = EmergencyRed, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.fullName, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                if (contact.phone.isNotBlank()) {
                    Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = EmergencyRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("إضافة جهة اتصال للطوارئ", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = MetallicBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmergencyRed,
                        unfocusedLabelColor = TextSecondary,
                    ),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = MetallicBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmergencyRed,
                        unfocusedLabelColor = TextSecondary,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name.trim(), phone.trim(), priority.toIntOrNull() ?: 1)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                enabled = name.isNotBlank() && phone.isNotBlank(),
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = MetallicSilver) }
        },
    )
}
