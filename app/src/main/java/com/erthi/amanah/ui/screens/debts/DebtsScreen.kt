package com.erthi.amanah.ui.screens.debts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*

@Composable
fun DebtsScreen(onBack: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { ErthiTopBar(title = "الديون", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SilverBlue,
                contentColor = NavyDeep,
            ) { Icon(Icons.Default.Add, null) }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SectionHeader("ما عليك من ديون")
            EmptyState("لا توجد ديون مسجّلة\nاضغط + لإضافة دين")
            SectionHeader("ما لك عند الآخرين")
            EmptyState("لا توجد مبالغ مستحقة\nاضغط + لإضافة مبلغ")
        }
    }
}
