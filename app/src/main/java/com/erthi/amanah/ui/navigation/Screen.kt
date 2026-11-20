package com.erthi.amanah.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object SetPin : Screen("set_pin")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Will : Screen("will")
    data object WillEditor : Screen("will_editor")
    data object WillAccess : Screen("will_access")
    data object Finance : Screen("finance")
    data object Debts : Screen("debts")
    data object Assets : Screen("assets")
    data object Entrusted : Screen("entrusted")
    data object DigitalAccounts : Screen("digital_accounts")
    data object Documents : Screen("documents")
    data object Beneficiaries : Screen("beneficiaries")
    data object Emergency : Screen("emergency")
    data object Backup : Screen("backup")
    data object Settings : Screen("settings")
    data object AboutApp : Screen("about_app")
    data object AboutErthi : Screen("about_erthi")
    // Password vault
    data object PasswordVault : Screen("password_vault")
    data object VaultItemAdd : Screen("vault_item_add")
    data class VaultItemEdit(val id: Long) : Screen("vault_item_edit/{id}") {
        companion object { const val ROUTE = "vault_item_edit/{id}" }
    }
    // More hub
    data object More : Screen("more")
    data object Feedback : Screen("feedback")
}
