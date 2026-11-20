package com.erthi.amanah.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erthi.amanah.ui.navigation.Screen
import com.erthi.amanah.ui.theme.*

data class DashboardSection(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String,
    val screen: Screen,
)

private val dashboardSections = listOf(
    DashboardSection(Icons.Default.Description, WillColor, "الوصية", "اكتب وصيتك وسجّلها صوتياً أو فيديو", Screen.Will),
    DashboardSection(Icons.Default.AccountBalance, FinanceColor, "الأموال", "الحسابات البنكية والأرصدة", Screen.Finance),
    DashboardSection(Icons.Default.CreditCard, DebtsColor, "الديون", "ما عليك من ديون وما لك عند الآخرين", Screen.Debts),
    DashboardSection(Icons.Default.HomeWork, AssetsColor, "الممتلكات", "العقارات والسيارات والأصول", Screen.Assets),
    DashboardSection(Icons.Default.VolunteerActivism, EntrustedColor, "الأمانات", "أمانات لديك تخص آخرين", Screen.Entrusted),
    DashboardSection(Icons.Default.Devices, DigitalColor, "الحسابات الرقمية", "البريد والحسابات والاشتراكات", Screen.DigitalAccounts),
    DashboardSection(Icons.Default.FolderOpen, DocumentsColor, "الوثائق", "المستندات والعقود المهمة", Screen.Documents),
    DashboardSection(Icons.Default.People, BeneficiariesColor, "الأسرة والمستفيدون", "بيانات من تتواصل معهم عند الحاجة", Screen.Beneficiaries),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إرثي أمانة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Emergency) }) {
                        Icon(Icons.Default.Warning, contentDescription = "الطوارئ", tint = EmergencyRed)
                    }
                    IconButton(onClick = { onNavigate(Screen.Settings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = SilverBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep),
            )
        },
        containerColor = NavyDeep,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NavyDeep)
        ) {
            // Welcome banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(NavyCard, NavyMid))
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "مرحباً بك",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "أدِر وصيتك وبياناتك المهمة بأمان",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(dashboardSections) { section ->
                    HomeSectionCard(
                        section = section,
                        onClick = { onNavigate(section.screen) },
                    )
                }
            }

            // Bottom navigation bar
            HomeBottomBar(
                onEmergency = { onNavigate(Screen.Emergency) },
                onBackup = { onNavigate(Screen.Backup) },
                onMore = { onNavigate(Screen.Settings) },
            )
        }
    }
}

@Composable
private fun HomeSectionCard(section: DashboardSection, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(section.iconColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = section.iconColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = section.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    onEmergency: () -> Unit,
    onBackup: () -> Unit,
    onMore: () -> Unit,
) {
    NavigationBar(
        containerColor = NavyMid,
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Home, contentDescription = null, tint = SilverBlue) },
            label = { Text("الرئيسية", color = SilverBlue) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onEmergency,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed) },
            label = { Text("الطوارئ", color = TextSecondary) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onBackup,
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BackupColor) },
            label = { Text("النسخ الاحتياطي", color = TextSecondary) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onMore,
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = TextSecondary) },
            label = { Text("المزيد", color = TextSecondary) },
        )
    }
}
