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
    butter = NoteSwatch(Color(0xFFFFD84D), Color(0xFF4A3D00)),
    mint = NoteSwatch(Color(0xFF7CE0A3), Color(0xFF1E4A30)),
    sky = NoteSwatch(Color(0xFF8CB8FF), Color(0xFF1F3A66)),
    peach = NoteSwatch(Color(0xFFFF9F7A), Color(0xFF5A2A14)),
    lilac = NoteSwatch(Color(0xFFC3A6FF), Color(0xFF3D2A70)),
    plain = NoteSwatch(Color(0xFFFFFFFF), Color(0xFF444444))
)

// Hue sama dengan versi terang, lightness diturunkan jauh agar kartu menyatu dengan latar gelap.
internal val DarkNoteColors = NoteColors(
    butter = NoteSwatch(Color(0xFF2E2914), Color(0xFFE8DDB8)),
    mint = NoteSwatch(Color(0xFF16291E), Color(0xFFC3E3CF)),
    sky = NoteSwatch(Color(0xFF172233), Color(0xFFC6D6F0)),
    peach = NoteSwatch(Color(0xFF301F18), Color(0xFFF0CDBE)),
    lilac = NoteSwatch(Color(0xFF231D36), Color(0xFFD9D0F5)),
    plain = NoteSwatch(Color(0xFF17171A), Color(0xFFA0A0A8))
)

internal val LocalNoteColors = staticCompositionLocalOf { LightNoteColors }
