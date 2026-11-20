package com.erthi.amanah.ui.screens.assets

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*

@Composable
fun AssetsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "الممتلكات", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(onClick = {}, containerColor = SilverBlue, contentColor = NavyDeep) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            EmptyState("لا توجد ممتلكات مسجّلة\nاضغط + لإضافة ممتلك")
        }
    }
}
