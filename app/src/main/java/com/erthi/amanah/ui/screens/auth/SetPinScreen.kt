package com.erthi.amanah.ui.screens.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.AuthViewModel

@Composable
fun SetPinScreen(
    authViewModel: AuthViewModel,
    onPinSet: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(NavyDeep)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Lock, null, tint = SilverBlue, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text("إنشاء رمز سري", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Text("سيُستخدم هذا الرمز لحماية بياناتك", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) { pin = it; errorMessage = null } },
                label = { Text("الرمز السري", color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = erthiTextFieldColors(),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 8) { confirmPin = it; errorMessage = null } },
                label = { Text("تأكيد الرمز السري", color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = erthiTextFieldColors(),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = EmergencyRed) } },
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    authViewModel.setPin(pin, confirmPin, onSuccess = onPinSet, onError = { errorMessage = it })
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SilverBlue, contentColor = NavyDeep),
                enabled = pin.length >= 4 && confirmPin.length >= 4,
            ) {
                Text("حفظ الرمز السري", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val canUseBiometric = remember {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val biometricPrompt = remember {
        val activity = context as FragmentActivity
        val executor = ContextCompat.getMainExecutor(activity)
        BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authViewModel.onBiometricSuccess()
                onAuthenticated()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                errorMessage = errString.toString()
            }
        })
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("إرثي أمانة")
            .setSubtitle("تأكيد الهوية بالبصمة")
            .setNegativeButtonText("استخدام الرمز السري")
            .build()
    }

    Box(modifier = Modifier.fillMaxSize().background(NavyDeep)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Lock, null, tint = SilverBlue, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text("تأكيد الهوية", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("أدخل الرمز السري للدخول", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) { pin = it; errorMessage = null } },
                label = { Text("الرمز السري", color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = erthiTextFieldColors(),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = EmergencyRed) } },
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    authViewModel.verifyPin(pin, onSuccess = onAuthenticated, onError = { errorMessage = it })
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SilverBlue, contentColor = NavyDeep),
                enabled = pin.length >= 4,
            ) {
                Text("دخول", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (canUseBiometric) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { biometricPrompt.authenticate(promptInfo) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SilverBlue),
                ) {
                    Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("الدخول بالبصمة", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun erthiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SilverBlue, unfocusedBorderColor = DividerColor,
    focusedLabelColor = SilverBlue, cursorColor = SilverBlue,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)
