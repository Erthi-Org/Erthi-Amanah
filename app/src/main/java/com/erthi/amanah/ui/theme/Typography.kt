@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.erthi.amanah.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.erthi.amanah.R

// Bundled variable font (OFL licence, sourced from google/fonts on GitHub).
// Works fully offline on first install — no GMS or network required.
// FontVariation.weight() sets the `wght` axis precisely for each Material 3 slot.
// minSdk = 26 satisfies the API-26 requirement for FontVariation.Settings.
val NotoKufiArabicFamily = FontFamily(
    Font(
        R.font.noto_kufi_arabic_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.noto_kufi_arabic_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.noto_kufi_arabic_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.noto_kufi_arabic_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val ErthiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = NotoKufiArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
