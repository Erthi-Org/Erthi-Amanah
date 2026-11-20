package com.erthi.amanah.ui.screens.beneficiaries

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*

@Composable
fun BeneficiariesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "الأسرة والمستفيدون", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(onClick = {}, containerColor = SilverBlue, contentColor = NavyDeep) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            EmptyState("لا توجد جهات اتصال مسجّلة")
        }
    }
}
