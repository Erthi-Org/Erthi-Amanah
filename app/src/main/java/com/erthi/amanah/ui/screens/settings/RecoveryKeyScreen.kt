package com.erthi.amanah.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.components.AndalusianCard
import com.erthi.amanah.ui.components.ErthiTopBar
import com.erthi.amanah.ui.theme.*

/**
 * Recovery Key information screen.
 *
 * The current architecture does NOT support a traditional standalone recovery key
 * because the encryption key is bound to Android Keystore (hardware-backed, non-exportable).
 * Recovery is possible through:
 *   1. Biometric unlock (if enrolled and configured)
 *   2. An encrypted backup export created by the user, restored by the user
 *      with the same passphrase on a new device.
 *
 * This screen explains the accurate recovery posture without false claims.
 */
@Composable
fun RecoveryKeyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { ErthiTopBar(title = "مفتاح الاسترداد", onBack = onBack) },
        containerColor = AndalusianBlue,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Honest status: no standalone recovery key exists yet
            AndalusianCard(accentColor = RoyalGold) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.VpnKey, null, tint = RoyalGold, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "كيف تعمل استعادة البيانات؟",
                            fontWeight = FontWeight.Bold,
                            color = RoyalGoldLight,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "لا يوجد في الإصدار الحالي مفتاح استرداد مستقل قابل للتصدير. التشفير مرتبط بـ Android Keystore الخاص بجهازك وهو غير قابل للاستخراج لأسباب أمنية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            AndalusianCard(accentColor = SuccessGreenLight) {
                Column {
                    Text(
                        "✅ خيار متاح: النسخة الاحتياطية المشفرة",
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreenLight,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "يمكنك إنشاء نسخة احتياطية مشفرة بكلمة مرور تختارها من قسم النسخ الاحتياطي، وحفظها على Google Drive أو جهاز تخزين شخصي. إذا فقدت هاتفك أو نسيت الرمز السري، يمكن استعادة بياناتك بكلمة المرور هذه على أي جهاز.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            AndalusianCard(accentColor = DigitalColor) {
                Column {
                    Text(
                        "✅ خيار متاح: البصمة والتعرف على الوجه",
                        fontWeight = FontWeight.SemiBold,
                        color = DigitalColor,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "إذا كان هاتفك يدعم المصادقة البيومترية وقمت بتفعيلها في التطبيق، يمكنك الدخول دون الرمز السري. البصمة مرتبطة بجهازك ولا تحل محل النسخة الاحتياطية على جهاز آخر.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            AndalusianCard(accentColor = PinkEnamel) {
                Column {
                    Text(
                        "⚠️ تحذير: نسيان الرمز السري مع غياب النسخة الاحتياطية",
                        fontWeight = FontWeight.SemiBold,
                        color = PinkEnamel,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "إذا نسيت الرمز السري ولم تُعِدّ نسخة احتياطية مشفرة ولا تعمل البصمة، سيكون الوصول إلى البيانات مستحيلاً. هذا تصميم متعمد لحماية خصوصيتك — لا توجد «باب خلفي».",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            AndalusianCard(accentColor = PurpleEnamelLight) {
                Column {
                    Text(
                        "🔮 مخطط مستقبلاً: مفتاح استرداد قابل للتصدير",
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleEnamelLight,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "يُدرس تنفيذ آلية مفتاح استرداد منفصلة (passphrase-derived، محمي بالمصادقة، مع تحذير واضح وتوثيق) في إصدار مستقبلي، تحت شرط ألا تُضعف بنية التشفير الحالية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
