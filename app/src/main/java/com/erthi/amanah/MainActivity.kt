package com.erthi.amanah

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.ui.navigation.ErthiNavGraph
import com.erthi.amanah.ui.theme.ErthiAmanaTheme
import com.erthi.amanah.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var authViewModel: AuthViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ErthiAmanaTheme {
                val vm: AuthViewModel = hiltViewModel()
                authViewModel = vm
                val authState by vm.authState.collectAsState()
                ErthiNavGraph(
                    authState = authState,
                    authViewModel = vm,
                )
            }
        }
    }

    // Lock on deliberate user navigation away (Home, Recents, Back-to-launcher).
    // onStop() is intentionally NOT used: it fires when any child activity launches
    // (e.g. the system file/document picker), which would force re-auth on every
    // attachment attempt. onUserLeaveHint() fires only on user-initiated background.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        authViewModel?.lock()
    }
}
