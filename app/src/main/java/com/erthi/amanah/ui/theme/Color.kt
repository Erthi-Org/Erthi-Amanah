package com.erthi.amanah.ui.theme

import androidx.compose.ui.graphics.Color

// ── Andalusian Royal Sapphire Palette ─────────────────────────────────────────
val AndalusianBlue      = Color(0xFF050C1F)   // deep royal sapphire — primary background
val AndalusianNavy      = Color(0xFF080F26)   // secondary surfaces
val AndalusianCard      = Color(0xFF0D152E)   // elevated cards
val AndalusianElevated  = Color(0xFF131B38)   // deeper card surfaces

// Royal Gold
val RoyalGold           = Color(0xFFC9A030)   // primary gold accent
val RoyalGoldLight      = Color(0xFFE8C85A)   // lighter gold highlights
val RoyalGoldDim        = Color(0xFF8C7020)   // muted gold for borders

// Purple & Pink Enamel accents
val PurpleEnamel        = Color(0xFF8B2FC9)   // jewel purple
val PinkEnamel          = Color(0xFFD4327A)   // jewel pink / rose
val PurpleEnamelLight   = Color(0xFFB565E0)   // lighter purple

// Andalusian geometric border
val AndalusianBorder    = Color(0xFF1E2D5E)   // subtle sapphire border
val AndalusianBorderGold= Color(0xFF3A2E10)   // subtle gold border

// ── Ultra-Deep Glossy Black Palette ──────────────────────────────────────────
val GlossyBlack      = AndalusianBlue          // primary background (now royal sapphire)
val GlossyBlackMid   = AndalusianNavy          // secondary surfaces
val ObsidianCard     = AndalusianCard          // elevated obsidian cards
val ObsidianElevated = AndalusianElevated      // deep reflective surfaces

// Aliases kept for all existing screens
val NavyDeep  = GlossyBlack
val NavyMid   = GlossyBlackMid
val NavyCard  = ObsidianCard
val NavyLight = ObsidianElevated

// ── Metallic Silver ───────────────────────────────────────────────────────────
val MetallicSilver  = Color(0xFFC0C0C0)
val PolishedSilver  = Color(0xFFE3E8EF)
val SilverHighlight = Color(0xFFF2F5FA)

// Aliases for existing screens
val SilverBlue  = MetallicSilver
val SilverLight = PolishedSilver
val SilverPale  = SilverHighlight

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0C4D8)
val TextMuted     = Color(0xFF5A6880)

// ── Gold Accent (Erthi brand) ─────────────────────────────────────────────────
val GoldAccent  = RoyalGold       // primary gold — now Andalusian royal gold
val GoldBright  = RoyalGoldLight  // bright gold highlights

// ── Semantic ─────────────────────────────────────────────────────────────────
val EmergencyRed      = Color(0xFFD32F2F)
val EmergencyRedLight = Color(0xFFEF5350)
val SuccessGreen      = Color(0xFF2E7D32)
val SuccessGreenLight = Color(0xFF4CAF50)
val WarningAmber      = Color(0xFFF57C00)

// ── Divider / metallic border ─────────────────────────────────────────────────
val DividerColor    = AndalusianBorder          // sapphire-toned divider
val MetallicBorder  = Color(0xFF1E2D5E)         // Andalusian sapphire border

// Legacy
val CardWhite     = Color(0xFFFFFFFF)
val CardLightBlue = Color(0xFFEBF4FC)

// ── Module accent colors ──────────────────────────────────────────────────────
val WillColor         = Color(0xFF7B61FF)   // purple
val FinanceColor      = Color(0xFFD9AA54)   // gold
val DebtsColor        = Color(0xFFEF5350)   // red
val AssetsColor       = Color(0xFF4CAF50)   // green
val EntrustedColor    = Color(0xFFFFA726)   // amber
val DigitalColor      = Color(0xFF29B6F6)   // cyan
val DocumentsColor    = Color(0xFF78909C)   // slate
val BeneficiariesColor= Color(0xFFEC407A)   // pink
val EmergencyColor    = Color(0xFFD32F2F)   // red
val BackupColor       = Color(0xFF26A69A)   // teal
val SettingsColor     = Color(0xFF90A4AE)   // blue-gray
val VaultColor        = Color(0xFF00B4D8)   // electric cyan  (password vault)
val VaultPurple       = Color(0xFF9C27B0)   // purple variant
