package com.erthi.amanah.ui.screens.home

import com.erthi.amanah.ui.components.OrnamentalIcon
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
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.erthi.amanah.R
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
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val screen: Screen,
)

private val dashboardSections = listOf(
    DashboardSection(Icons.Default.HistoryEdu, WillColor, R.string.nav_will, R.string.will_subtitle, Screen.Will),
    DashboardSection(Icons.Default.AccountBalance, FinanceColor, R.string.nav_finance, R.string.finance_subtitle, Screen.Finance),
    DashboardSection(Icons.Default.CreditCard, DebtsColor, R.string.nav_debts, R.string.debts_owed_title, Screen.Debts),
    DashboardSection(Icons.Default.HomeWork, AssetsColor, R.string.nav_assets, R.string.assets_subtitle, Screen.Assets),
    DashboardSection(Icons.Default.VolunteerActivism, EntrustedColor, R.string.nav_entrusted, R.string.entrusted_subtitle, Screen.Entrusted),
    DashboardSection(Icons.Default.Devices, DigitalColor, R.string.nav_digital, R.string.digital_subtitle, Screen.DigitalAccounts),
    DashboardSection(Icons.Default.FolderOpen, DocumentsColor, R.string.nav_documents, R.string.documents_subtitle, Screen.Documents),
    DashboardSection(Icons.Default.People, BeneficiariesColor, R.string.nav_beneficiaries, R.string.beneficiaries_subtitle, Screen.Beneficiaries),
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
                        Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(stringResource(R.string.about_app_desc), fontSize = 10.sp, color = GoldAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.More) }) {
                        OrnamentalIcon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_more), tint = MetallicSilver)
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
                        OrnamentalIcon(Icons.Default.Shield, null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(R.string.welcome_title), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(stringResource(R.string.welcome_subtitle), fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(dashboardSections) { section ->
                    AndalusianSectionCard(
                        icon = section.icon,
                        iconColor = section.iconColor,
                        title = stringResource(section.title),
                        subtitle = stringResource(section.subtitle),
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
            icon = { OrnamentalIcon(Icons.Default.Home, null, tint = RoyalGold) },
            label = { Text(stringResource(R.string.nav_home), style = MaterialTheme.typography.labelSmall, color = RoyalGold) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = RoyalGold.copy(alpha = 0.15f)),
        )
        NavigationBarItem(
            selected = false,
            onClick = onVault,
            icon = { OrnamentalIcon(Icons.Default.Lock, null, tint = PurpleEnamelLight) },
            label = { Text(stringResource(R.string.nav_vault), style = MaterialTheme.typography.labelSmall, color = TextSecondary) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = PurpleEnamel.copy(alpha = 0.12f)),
        )
        NavigationBarItem(
            selected = false,
            onClick = onBackup,
            icon = { OrnamentalIcon(Icons.Default.CloudUpload, null, tint = BackupColor) },
            label = { Text(stringResource(R.string.nav_backup), style = MaterialTheme.typography.labelSmall, color = TextSecondary) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = BackupColor.copy(alpha = 0.12f)),
        )
        NavigationBarItem(
            selected = false,
            onClick = onMore,
            icon = { OrnamentalIcon(Icons.Default.MoreHoriz, null, tint = TextSecondary) },
            label = { Text(stringResource(R.string.nav_more), style = MaterialTheme.typography.labelSmall, color = TextSecondary) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
        )
    }
}
