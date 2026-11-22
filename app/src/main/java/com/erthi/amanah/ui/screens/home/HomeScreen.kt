package com.erthi.amanah.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import com.erthi.amanah.ui.components.AndalusianSectionCard
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
    DashboardSection(Icons.Default.HistoryEdu, WillColor, "الوصية", "اكتب وصيتك وسجّلها", Screen.Will),
    DashboardSection(Icons.Default.AccountBalance, FinanceColor, "الأموال", "الحسابات البنكية", Screen.Finance),
    DashboardSection(Icons.Default.CreditCard, DebtsColor, "الديون", "ما عليك وما لك", Screen.Debts),
    DashboardSection(Icons.Default.HomeWork, AssetsColor, "الممتلكات", "العقارات والأصول", Screen.Assets),
    DashboardSection(Icons.Default.VolunteerActivism, EntrustedColor, "الأمانات", "أمانات لديك", Screen.Entrusted),
    DashboardSection(Icons.Default.Devices, DigitalColor, "الحسابات الرقمية", "البريد والاشتراكات", Screen.DigitalAccounts),
    DashboardSection(Icons.Default.FolderOpen, DocumentsColor, "الوثائق", "المستندات المهمة", Screen.Documents),
    DashboardSection(Icons.Default.People, BeneficiariesColor, "الأسرة والمستفيدون", "جهات التواصل", Screen.Beneficiaries),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    currentTab: Int = 0,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("وصيتي", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("أحد مشروعات إرثي الخيرية", fontSize = 10.sp, color = GoldAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.More) }) {
                        Icon(Icons.Default.Settings, contentDescription = "المزيد", tint = MetallicSilver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GlossyBlack),
            )
        },
        containerColor = GlossyBlack,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(GlossyBlack),
        ) {
            // Compact welcome banner
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0D1A10), GlossyBlackMid)))
                    .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Shield, null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("مرحباً بك", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("بياناتك مشفّرة ومحفوظة على هاتفك", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                // Bottom padding ensures last row is fully visible above bottom nav + system bar.
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 8.dp,
                    bottom = 88.dp,  // nav bar height (~56dp) + 32dp breathing room
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(dashboardSections) { section ->
                    AndalusianSectionCard(
                        icon = section.icon,
                        iconColor = section.iconColor,
                        title = section.title,
                        subtitle = section.subtitle,
                        onClick = { onNavigate(section.screen) },
                    )
                }
            }

            HomeBottomBar(
                onVault = { onNavigate(Screen.PasswordVault) },
                onBackup = { onNavigate(Screen.Backup) },
                onMore = { onNavigate(Screen.More) },
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    onVault: () -> Unit,
    onBackup: () -> Unit,
    onMore: () -> Unit,
) {
    NavigationBar(
        containerColor = AndalusianNavy,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, RoyalGold.copy(alpha = 0.3f), Color.Transparent)
            ),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        ),
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, null, tint = RoyalGold) },
            label = { Text("الرئيسية", style = MaterialTheme.typography.labelSmall, color = RoyalGold) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = RoyalGold.copy(alpha = 0.15f)),
        )
        NavigationBarItem(
            selected = false,
            onClick = onVault,
            icon = { Icon(Icons.Default.Lock, null, tint = PurpleEnamelLight) },
            label = { Text("الخزنة", style = MaterialTheme.typography.labelSmall, color = TextSecondary) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = PurpleEnamel.copy(alpha = 0.12f)),
        )
        NavigationBarItem(
            selected = false,
            onClick = onBackup,
            icon = { Icon(Icons.Default.CloudUpload, null, tint = BackupColor) },
            label = { Text("النسخ", style = MaterialTheme.typography.labelSmall, color = TextSecondary) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BackupColor.copy(alpha = 0.12f)),
        )
        NavigationBarItem(
            selected = false,
            onClick = onMore,
            icon = { Icon(Icons.Default.MoreHoriz, null, tint = TextSecondary) },
            label = { Text("المزيد", style = MaterialTheme.typography.labelSmall, color = TextSecondary) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
        )
    }
}
