package com.erthi.amanah.ui.screens.finance

import com.erthi.amanah.ui.components.OrnamentalIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

@Composable
fun FinanceScreen(
    onBack: () -> Unit,
    onNavigateDebts: () -> Unit,
    onNavigateAssets: () -> Unit,
    onNavigateDigitalAccounts: () -> Unit = {},
) {
    Scaffold(
        topBar = { ErthiTopBar(title = "الأموال", onBack = onBack) },
        containerColor = NavyDeep,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FinanceCard("الديون", "ما عليك من ديون وما لك عند الآخرين", Icons.Default.CreditCard, DebtsColor, onNavigateDebts)
            FinanceCard("الممتلكات", "العقارات والسيارات والأصول", Icons.Default.HomeWork, AssetsColor, onNavigateAssets)
            FinanceCard("الحسابات الرقمية", "الحسابات البنكية والاشتراكات الرقمية", Icons.Default.AccountBalance, FinanceColor, onNavigateDigitalAccounts)
        }
    }
}

@Composable
private fun FinanceCard(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OrnamentalIcon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            OrnamentalIcon(Icons.Default.ChevronRight, null, tint = TextMuted)
        }
    }
}
