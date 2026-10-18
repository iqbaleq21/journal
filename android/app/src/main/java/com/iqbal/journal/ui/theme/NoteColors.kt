package com.iqbal.journal.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.iqbal.journal.domain.model.NoteColor

/** Pasangan warna latar kartu catatan dan teks sekunder di atasnya. */
@Immutable
data class NoteSwatch(val container: Color, val onContainerMuted: Color)

/** Palet catatan; kuncinya sama dengan nilai kolom `notes.color` di database. */
@Immutable
data class NoteColors(
    val butter: NoteSwatch,
    val mint: NoteSwatch,
    val sky: NoteSwatch,
    val peach: NoteSwatch,
    val lilac: NoteSwatch,
    val plain: NoteSwatch
)

fun NoteColors.swatchFor(color: NoteColor): NoteSwatch = when (color) {
    NoteColor.Butter -> butter
    NoteColor.Mint -> mint
    NoteColor.Sky -> sky
    NoteColor.Peach -> peach
    NoteColor.Lilac -> lilac
    NoteColor.Plain -> plain
}

internal val LightNoteColors = NoteColors(
    butter = NoteSwatch(Color(0xFFFFE7A3), Color(0xFF5A4A20)),
    mint = NoteSwatch(Color(0xFFC9F0D8), Color(0xFF2F4A3A)),
    sky = NoteSwatch(Color(0xFFCDE2FF), Color(0xFF3E4A60)),
    peach = NoteSwatch(Color(0xFFFFD3BF), Color(0xFF4D4040)),
    lilac = NoteSwatch(Color(0xFFE2D8FF), Color(0xFF4A4566)),
    plain = NoteSwatch(Color(0xFFFFFFFF), Color(0xFF5E5B6B))
)

// Hue sama dengan versi terang, lightness diturunkan agar teks terang tetap kontras.
internal val DarkNoteColors = NoteColors(
    butter = NoteSwatch(Color(0xFF4A3F1C), Color(0xFFE8DDB8)),
    mint = NoteSwatch(Color(0xFF1F3B2C), Color(0xFFC3E3CF)),
    sky = NoteSwatch(Color(0xFF1E2F4A), Color(0xFFC6D6F0)),
    peach = NoteSwatch(Color(0xFF4A2E22), Color(0xFFF0CDBE)),
    lilac = NoteSwatch(Color(0xFF2F2752), Color(0xFFD9D0F5)),
    plain = NoteSwatch(Color(0xFF24232B), Color(0xFFC9C5D8))
)

internal val LocalNoteColors = staticCompositionLocalOf { LightNoteColors }
