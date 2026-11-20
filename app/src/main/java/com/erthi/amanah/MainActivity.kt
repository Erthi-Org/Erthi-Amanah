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

    override fun onStop() {
        super.onStop()
        authViewModel?.lock()
    }
}
