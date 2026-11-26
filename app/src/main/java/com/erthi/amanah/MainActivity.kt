package com.erthi.amanah

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.content.ContextCompat
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

    private val handler = Handler(Looper.getMainLooper())
    private val picker = PickerSession()
    private val pickerTimeout = Runnable { lockSession() }
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lockSession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        ContextCompat.registerReceiver(
            this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        val isFilePicker = requestCode >= 0 && (
            intent.action == Intent.ACTION_OPEN_DOCUMENT || intent.action == Intent.ACTION_CREATE_DOCUMENT
        )
        if (isFilePicker) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            picker.begin(requestCode, SystemClock.elapsedRealtime())
            handler.removeCallbacks(pickerTimeout)
            handler.postDelayed(pickerTimeout, PickerSession.TIMEOUT_MS)
        }
        try {
            super.startActivityForResult(intent, requestCode, options)
        } catch (e: RuntimeException) {
            if (isFilePicker) clearPicker()
            throw e
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (picker.isExpectedResult(requestCode)) {
            if (!picker.isActive(SystemClock.elapsedRealtime())) lockSession()
            clearPicker()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        lockSession()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !picker.isActive(SystemClock.elapsedRealtime())) {
            lockSession()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        if (picker.isActive(SystemClock.elapsedRealtime())) lockSession() else clearPicker()
        super.onDestroy()
    }

    private fun clearPicker() {
        handler.removeCallbacks(pickerTimeout)
        picker.clear()
    }

    private fun lockSession() {
        clearPicker()
        authViewModel?.lock()
    }
}
