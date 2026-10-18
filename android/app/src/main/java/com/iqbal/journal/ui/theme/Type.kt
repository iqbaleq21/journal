package com.iqbal.journal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.iqbal.journal.R

// Kedua font adalah variable font; satu file dipetakan ke beberapa berat.
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

internal val BricolageGrotesque = FontFamily(
    listOf(FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
        .map { variableFont(R.font.bricolage_grotesque, it) }
)

internal val Figtree = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
        .map { variableFont(R.font.figtree, it) }
)

private fun display(size: Int, weight: FontWeight, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = BricolageGrotesque,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = letterSpacing.sp
)

private fun text(size: Int, weight: FontWeight, lineHeight: Int = (size * 1.45).toInt()) = TextStyle(
    fontFamily = Figtree,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

internal val JournalTypography = Typography(
    displayLarge = display(54, FontWeight.ExtraBold, letterSpacing = -1.5),
    displayMedium = display(38, FontWeight.Bold, letterSpacing = -0.8),
    displaySmall = display(34, FontWeight.Bold, letterSpacing = -0.6),
    headlineLarge = display(30, FontWeight.Bold, letterSpacing = -0.6),
    headlineMedium = display(26, FontWeight.SemiBold, letterSpacing = -0.3),
    headlineSmall = display(23, FontWeight.Bold, letterSpacing = -0.3),
    titleLarge = display(24, FontWeight.Bold, letterSpacing = -0.3),
    titleMedium = text(17, FontWeight.Bold),
    titleSmall = text(15, FontWeight.Bold),
    bodyLarge = text(16, FontWeight.Medium),
    bodyMedium = text(15, FontWeight.Medium),
    bodySmall = text(13, FontWeight.Normal),
    labelLarge = text(16, FontWeight.Bold),
    labelMedium = text(13, FontWeight.SemiBold),
    labelSmall = text(12, FontWeight.Bold)
)
