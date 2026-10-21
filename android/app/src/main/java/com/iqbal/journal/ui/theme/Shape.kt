package com.iqbal.journal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val JournalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp), // input
    medium = RoundedCornerShape(18.dp), // kartu catatan
    large = RoundedCornerShape(24.dp), // kartu besar, widget
    extraLarge = RoundedCornerShape(24.dp) // kartu aksi utama
)

/** FAB memakai rounded square, bukan lingkaran. */
val FabShape = RoundedCornerShape(20.dp)
