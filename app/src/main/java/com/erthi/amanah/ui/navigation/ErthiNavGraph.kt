package com.erthi.amanah.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.erthi.amanah.ui.screens.about.AboutAppScreen
import com.erthi.amanah.ui.screens.about.AboutErthiScreen
import com.erthi.amanah.ui.screens.settings.EncryptionInfoScreen
import com.erthi.amanah.ui.screens.settings.LanguagePickerScreen
import com.erthi.amanah.ui.screens.settings.PrivacyScreen
import com.erthi.amanah.ui.screens.settings.RecoveryKeyScreen
import com.erthi.amanah.ui.screens.assets.AssetsScreen
import com.erthi.amanah.ui.screens.auth.AuthScreen
import com.erthi.amanah.ui.screens.auth.SetPinScreen
import com.erthi.amanah.ui.screens.backup.BackupScreen
import com.erthi.amanah.ui.screens.beneficiaries.BeneficiariesScreen
import com.erthi.amanah.ui.screens.debts.DebtsScreen
import com.erthi.amanah.ui.screens.digital.DigitalAccountsScreen
import com.erthi.amanah.ui.screens.documents.DocumentsScreen
import com.erthi.amanah.ui.screens.emergency.EmergencyScreen
import com.erthi.amanah.ui.screens.entrusted.EntrustedScreen
import com.erthi.amanah.ui.screens.feedback.FeedbackScreen
import com.erthi.amanah.ui.screens.finance.FinanceScreen
import com.erthi.amanah.ui.screens.home.HomeScreen
import com.erthi.amanah.ui.screens.more.MoreScreen
import com.erthi.amanah.ui.screens.onboarding.OnboardingScreen
import com.erthi.amanah.ui.screens.settings.SettingsScreen
import com.erthi.amanah.ui.screens.vault.PasswordVaultScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.erthi.amanah.ui.screens.will.AudioWillScreen
import com.erthi.amanah.ui.screens.will.VideoWillPlayerScreen
import com.erthi.amanah.ui.screens.will.VideoWillScreen
import com.erthi.amanah.ui.screens.will.WillAccessScreen
import com.erthi.amanah.ui.screens.will.WillEditorScreen
import com.erthi.amanah.ui.screens.will.WillScreen
import com.erthi.amanah.ui.viewmodel.AuthState
import com.erthi.amanah.ui.viewmodel.AuthViewModel

@Composable
fun ErthiNavGraph(
    authState: AuthState,
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = when (authState) {
        AuthState.FirstLaunch -> Screen.Onboarding.route
        AuthState.PinNotSet -> Screen.SetPin.route
        AuthState.Locked -> Screen.Auth.route
        AuthState.Unlocked -> Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onGetStarted = { navController.navigate(Screen.SetPin.route) }
            )
        }

        composable(Screen.SetPin.route) {
            SetPinScreen(
                authViewModel = authViewModel,
                onPinSet = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetPin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { screen -> navController.navigate(screen.route) }
            )
        }

        composable(Screen.Will.route) {
            WillScreen(
                onBack = { navController.popBackStack() },
                onEditWill = { navController.navigate(Screen.WillEditor.route) },
                onWillAccess = { navController.navigate(Screen.WillAccess.route) },
                onAudioWill = { navController.navigate(Screen.AudioWill.route) },
                onVideoWill = { navController.navigate(Screen.VideoWill.route) },
                onPlayVideo = { path ->
                    navController.navigate(
                        "video_will_player/${java.net.URLEncoder.encode(path, "UTF-8")}")
                },
            )
        }

        composable(Screen.WillEditor.route) {
            WillEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Finance.route) {
            FinanceScreen(
                onBack = { navController.popBackStack() },
                onNavigateDebts = { navController.navigate(Screen.Debts.route) },
                onNavigateAssets = { navController.navigate(Screen.Assets.route) },
                onNavigateDigitalAccounts = { navController.navigate(Screen.DigitalAccounts.route) },
            )
        }

        composable(Screen.Debts.route) {
            DebtsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Assets.route) {
            AssetsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Entrusted.route) {
            EntrustedScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.DigitalAccounts.route) {
            DigitalAccountsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Documents.route) {
            DocumentsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Beneficiaries.route) {
            BeneficiariesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Emergency.route) {
            EmergencyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Backup.route) {
            BackupScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateAboutApp = { navController.navigate(Screen.AboutApp.route) },
                onNavigateAboutErthi = { navController.navigate(Screen.AboutErthi.route) },
                onLockApp = {
                    authViewModel.lock()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigatePrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateEncryption = { navController.navigate(Screen.EncryptionInfo.route) },
                onNavigateRecoveryKey = { navController.navigate(Screen.RecoveryKey.route) },
                onNavigateDeathVerification = { /* Coming soon — no-op */ },
                onNavigateLanguagePicker = { navController.navigate(Screen.LanguagePicker.route) },
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.EncryptionInfo.route) {
            EncryptionInfoScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.RecoveryKey.route) {
            RecoveryKeyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.LanguagePicker.route) {
            LanguagePickerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AboutApp.route) {
            AboutAppScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AboutErthi.route) {
            AboutErthiScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.PasswordVault.route) {
            PasswordVaultScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.More.route) {
            MoreScreen(
                onBack = { navController.popBackStack() },
                onNavigateEmergency = { navController.navigate(Screen.Emergency.route) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateAboutApp = { navController.navigate(Screen.AboutApp.route) },
                onNavigateAboutErthi = { navController.navigate(Screen.AboutErthi.route) },
                onNavigateFeedback = { navController.navigate(Screen.Feedback.route) },
            )
        }

        composable(Screen.Feedback.route) {
            FeedbackScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.WillAccess.route) {
            WillAccessScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AudioWill.route) {
            AudioWillScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.VideoWill.route) {
            VideoWillScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.VideoWillPlayer.ROUTE,
            arguments = listOf(navArgument("path") {
                type = NavType.StringType
                nullable = false
            }),
        ) { backStackEntry ->
            val path = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("path") ?: "", "UTF-8")
            VideoWillPlayerScreen(
                encryptedVideoPath = path,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
