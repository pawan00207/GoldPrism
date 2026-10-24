package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PROFESSIONAL TRADING TERMINAL PALETTE (Phase 6)
// Minimal, flat, dense design inspired by professional broker terminals
// ==========================================

// Light Theme Tokens (Default)
val TerminalBgLight = Color(0xFFFFFFFF)
val TerminalSurfaceAltLight = Color(0xFFF9F9F9)
val TerminalDividerLight = Color(0xFFEEEEEE)
val TerminalBorderLight = Color(0xFFE0E0E0)
val TerminalTextPrimaryLight = Color(0xFF424242)
val TerminalTextSecondaryLight = Color(0xFF9B9B9B)
val TerminalAccentLight = Color(0xFF387ED1)
val TerminalPositiveLight = Color(0xFF4CAF50)
val TerminalNegativeLight = Color(0xFFDF514C)
val TerminalWarningLight = Color(0xFFFF9800)
val TerminalBuyLight = Color(0xFF4184F3)
val TerminalSellLight = Color(0xFFFF5722)

// Dark Theme Tokens (Optional / System Dark)
val TerminalBgDark = Color(0xFF121212)
val TerminalSurfaceAltDark = Color(0xFF1B1B1B)
val TerminalDividerDark = Color(0xFF2A2A2A)
val TerminalBorderDark = Color(0xFF333333)
val TerminalTextPrimaryDark = Color(0xFFE6E6E6)
val TerminalTextSecondaryDark = Color(0xFF8A8A8A)
val TerminalAccentDark = Color(0xFF4A8FE7)
val TerminalPositiveDark = Color(0xFF52B756)
val TerminalNegativeDark = Color(0xFFE55C57)
val TerminalWarningDark = Color(0xFFFFA726)
val TerminalBuyDark = Color(0xFF4A8FE7)
val TerminalSellDark = Color(0xFFFF6E40)

// Aliases mapped cleanly to terminal tokens to ensure zero breaking changes in screens:
val ObsidianBackground = TerminalBgDark
val CharcoalSurface = TerminalSurfaceAltDark
val CharcoalSurfaceVariant = TerminalSurfaceAltDark
val TerminalBorder = TerminalBorderDark
val TerminalBorderSubtle = TerminalDividerDark

val GoldPrimary = TerminalAccentLight
val GoldLight = Color(0xFF64B5F6)
val GoldDark = Color(0xFF1E88E5)
val GoldContainer = TerminalSurfaceAltDark
val OnGoldContainer = TerminalTextPrimaryDark

val PrismCyan = Color(0xFF26A69A)
val PrismCyanContainer = Color(0xFF004D40)
val PrismViolet = Color(0xFF7E57C2)
val PrismVioletContainer = Color(0xFF311B92)

val EmeraldProfit = TerminalPositiveLight
val EmeraldContainer = Color(0xFF1B5E20)
val CrimsonLoss = TerminalNegativeLight
val CrimsonContainer = Color(0xFFB71C1C)
val AmberWarning = TerminalWarningLight
val AmberContainer = Color(0xFFE65100).copy(alpha = 0.25f)
val SlateNoEdge = Color(0xFF757575)

val TextWhitePrimary = TerminalTextPrimaryDark
val TextMutedSecondary = TerminalTextSecondaryDark
val TextSubtleTertiary = Color(0xFF616161)
