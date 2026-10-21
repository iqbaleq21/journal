package com.iqbal.journal.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Garis dan bayangan kartu: tegas di tema terang (neo-brutalism), tipis tanpa bayangan di tema gelap. */
@Immutable
data class CardStyle(val border: BorderStroke, val shadowOffset: Dp, val shadowColor: Color)

internal val LightCardStyle = CardStyle(BorderStroke(2.dp, Color(0xFF111111)), 4.dp, Color(0xFF111111))
internal val DarkCardStyle = CardStyle(BorderStroke(1.dp, Color(0xFF2A2A30)), 0.dp, Color.Transparent)

internal val LocalCardStyle = staticCompositionLocalOf { LightCardStyle }

/** Bayangan padat bergeser ke kanan-bawah; pasang sebelum `border`/`background` agar tergambar di belakang. */
@Composable
fun Modifier.hardShadow(shape: Shape): Modifier {
    val style = JournalTheme.cardStyle
    if (style.shadowOffset == 0.dp) return this
    return drawBehind {
        val offset = style.shadowOffset.toPx()
        translate(offset, offset) { drawOutline(shape.createOutline(size, layoutDirection, this), style.shadowColor) }
    }
}
