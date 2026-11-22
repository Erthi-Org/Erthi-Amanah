package com.erthi.amanah.ui.screens.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.erthi.amanah.ui.components.AndalusianLockEmblem
import com.erthi.amanah.ui.components.QuranVerseFrame
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.AuthViewModel

// Quran excerpt — البقرة 180 (مقتطف). Do not alter the Arabic text.
private const val QURAN_VERSE =
    "﴿كُتِبَ عَلَيْكُمْ إِذَا حَضَرَ أَحَدَكُمُ الْمَوْتُ\nإِنْ تَرَكَ خَيْرًا الْوَصِيَّةُ﴾"
private const val QURAN_ATTRIBUTION = "سورة البقرة — الآية 180 (مقتطف)"

@Composable
fun SetPinScreen(
    authViewModel: AuthViewModel,
    onPinSet: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var acknowledged by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AndalusianBlue, AndalusianNavy, AndalusianCard))
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Quranic verse frame
            QuranVerseFrame(verse = QURAN_VERSE, attribution = QURAN_ATTRIBUTION)
            Spacer(Modifier.height(28.dp))

            // Ornamental lock emblem
            AndalusianLockEmblem(size = 76.dp, locked = true)
            Spacer(Modifier.height(16.dp))

            Text(
                "إنشاء رمز سري",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                "أدخل رمزاً سرياً لحماية بياناتك",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) { pin = it; errorMessage = null } },
                label = { Text("الرمز السري", color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = andalusianFieldColors(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 8) { confirmPin = it; errorMessage = null } },
                label = { Text("تأكيد الرمز السري", color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = andalusianFieldColors(),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = EmergencyRed) } },
            )

            // Security warning
            Spacer(Modifier.height(18.dp))
            Surface(
                color = RoyalGold.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, null, tint = RoyalGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        // Accurate warning: describes the actual recovery architecture.
                        // Data is locally encrypted; PIN loss means loss of access unless
                        // the user has set up biometric unlock or an exported backup.
                        "بياناتك محفوظة بتشفير محلي لحماية خصوصيتك. إذا نسيت الرمز السري ولم تُعِدّ " +
                            "نسخة احتياطية مشفرة، فقد تفقد الوصول إلى بياناتك. احتفظ بالرمز في " +
                            "مكان آمن وأنشئ نسخة احتياطية مشفرة بانتظام.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoyalGoldLight,
                    )
                }
            }

            // Acknowledgment checkbox
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = acknowledged,
                    onCheckedChange = { acknowledged = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = RoyalGold,
                        uncheckedColor = TextMuted,
                        checkmarkColor = AndalusianBlue,
                    ),
                )
                Text(
                    "فهمت وأوافق على المتابعة",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Gold gradient CTA button
            Button(
                onClick = {
                    authViewModel.setPin(pin, confirmPin, onSuccess = onPinSet, onError = { errorMessage = it })
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = AndalusianBlue,
                    disabledContainerColor = RoyalGold.copy(alpha = 0.3f),
                    disabledContentColor = AndalusianBlue.copy(alpha = 0.5f),
                ),
                enabled = pin.length >= 4 && confirmPin.length >= 4 && acknowledged,
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("حفظ الرمز السري", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(32.dp))
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AndalusianBlue, AndalusianNavy, AndalusianCard))
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Quranic verse at top
            QuranVerseFrame(
                verse = QURAN_VERSE,
                attribution = QURAN_ATTRIBUTION,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))

            // Ornamental lock
            AndalusianLockEmblem(size = 80.dp, locked = true)
            Spacer(Modifier.height(16.dp))

            Text(
                "تأكيد الهوية",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                "أدخل الرمز السري للدخول",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) { pin = it; errorMessage = null } },
                label = { Text("الرمز السري", color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = andalusianFieldColors(),
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = AndalusianBlue,
                    disabledContainerColor = RoyalGold.copy(alpha = 0.3f),
                    disabledContentColor = AndalusianBlue.copy(alpha = 0.5f),
                ),
                enabled = pin.length >= 4,
            ) {
                Text("دخول", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (canUseBiometric) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { biometricPrompt.authenticate(promptInfo) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold),
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
private fun andalusianFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RoyalGold,
    unfocusedBorderColor = AndalusianBorder,
    focusedLabelColor = RoyalGold,
    cursorColor = RoyalGold,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)
