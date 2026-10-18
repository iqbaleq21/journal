package com.iqbal.journal.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.iqbal.journal.MainActivity
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.ui.common.formatRelativeTime
import com.iqbal.journal.ui.navigation.NoteLink
import com.iqbal.journal.ui.navigation.openNoteIntent
import com.iqbal.journal.ui.theme.DarkColorScheme
import com.iqbal.journal.ui.theme.DarkNoteColors
import com.iqbal.journal.ui.theme.LightColorScheme
import com.iqbal.journal.ui.theme.LightNoteColors
import com.iqbal.journal.ui.theme.swatchFor
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/** Widget satu catatan. Datanya hanya dari Room lewat [NoteWidgetStateLoader]. */
class NoteWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val states = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            .stateLoader()
            .observe(appWidgetId)
        // Nilai awal dibaca dulu agar widget tidak sempat tampil kosong.
        val initial = states.first()
        provideContent {
            // Dikumpulkan di dalam komposisi: selama sesi Glance hidup, update() hanya me-recompose.
            val state by states.collectAsState(initial)
            NoteWidgetContent(state = state, appWidgetId = appWidgetId)
        }
    }
}

// Ukuran widget dari mockup DESIGN.md.
private val CornerRadius = 28.dp
private val ContentPadding = 16.dp
private val SmallGap = 4.dp
private val IconSize = 20.dp
private val WideMinWidth = 250.dp
private val TallMinHeight = 200.dp
private const val CONTENT_SIZE_SMALL = 18
private const val CONTENT_SIZE_LARGE = 22
private const val META_SIZE = 12
private const val MESSAGE_SIZE = 14
private const val MAX_LINES_SHORT = 3
private const val MAX_LINES_TALL = 8

private val InkColor = ColorProvider(day = LightColorScheme.onSurface, night = DarkColorScheme.onSurface)

@Composable
private fun NoteWidgetContent(state: NoteWidgetState, appWidgetId: Int) {
    val context = LocalContext.current
    val pickNote = actionStartActivity(context.widgetConfigIntent(appWidgetId))
    when (state) {
        is NoteWidgetState.Showing -> NoteContent(state = state, appWidgetId = appWidgetId)

        NoteWidgetState.NeedsProfile -> Message(
            text = context.getString(R.string.widget_needs_profile),
            onClick = actionStartActivity(Intent(context, MainActivity::class.java))
        )

        NoteWidgetState.NotConfigured -> Message(context.getString(R.string.widget_not_configured), pickNote)

        NoteWidgetState.LeftGroup -> Message(context.getString(R.string.widget_left_group), pickNote)

        NoteWidgetState.NoteDeleted -> Message(
            context.getString(R.string.widget_note_deleted),
            pickNote,
            showDeleteIcon = true
        )
    }
}

@Composable
private fun NoteContent(state: NoteWidgetState.Showing, appWidgetId: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val note = state.note
    val isWide = size.width >= WideMinWidth
    val author = when {
        state.isMine -> context.getString(R.string.note_author_you)
        else -> note.authorName ?: context.getString(R.string.note_author_unknown)
    }
    val meta = if (isWide) {
        context.getString(
            R.string.widget_meta_wide,
            author,
            state.groupName,
            context.formatRelativeTime(note.updatedAt)
        )
    } else {
        context.getString(R.string.widget_meta, author, state.groupName)
    }
    val muted = mutedColor(note.color)

    Column(
        modifier = cardModifier(note.color)
            .clickable(actionStartActivity(context.openNoteIntent(NoteLink(note.groupId, note.id))))
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            Text(
                text = note.content,
                maxLines = if (size.height >= TallMinHeight) MAX_LINES_TALL else MAX_LINES_SHORT,
                style = TextStyle(
                    color = InkColor,
                    fontSize = (if (isWide) CONTENT_SIZE_LARGE else CONTENT_SIZE_SMALL).sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            // Android 12+ memakai "Atur ulang" dari launcher; versi lama butuh tombol sendiri (PRD §5.4).
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Image(
                    provider = ImageProvider(R.drawable.ic_swap),
                    contentDescription = context.getString(R.string.widget_change),
                    modifier = GlanceModifier
                        .size(IconSize)
                        .clickable(actionStartActivity(context.widgetConfigIntent(appWidgetId)))
                )
            }
        }
        Spacer(GlanceModifier.height(SmallGap))
        Text(
            text = meta,
            maxLines = 1,
            style = TextStyle(color = muted, fontSize = META_SIZE.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun Message(text: String, onClick: Action, showDeleteIcon: Boolean = false) {
    Column(
        verticalAlignment = Alignment.Bottom,
        modifier = cardModifier(NoteColor.Plain).clickable(onClick)
    ) {
        if (showDeleteIcon) {
            Image(
                provider = ImageProvider(R.drawable.ic_delete),
                contentDescription = null,
                modifier = GlanceModifier.size(IconSize)
            )
            Spacer(GlanceModifier.defaultWeight())
        }
        Text(
            text = text,
            style = TextStyle(color = InkColor, fontSize = MESSAGE_SIZE.sp, fontWeight = FontWeight.Medium)
        )
    }
}

/** Sudut membulat hanya didukung Android 12+; versi lama tampil bersudut. */
private fun cardModifier(color: NoteColor): GlanceModifier = GlanceModifier
    .fillMaxSize()
    .background(
        ColorProvider(
            day = LightNoteColors.swatchFor(color).container,
            night = DarkNoteColors.swatchFor(color).container
        )
    )
    .cornerRadius(CornerRadius)
    .padding(ContentPadding)

private fun mutedColor(color: NoteColor): ColorProvider = ColorProvider(
    day = LightNoteColors.swatchFor(color).onContainerMuted,
    night = DarkNoteColors.swatchFor(color).onContainerMuted
)

/** Data URI unik per widget agar PendingIntent antar-widget tidak saling menimpa. */
internal fun Context.widgetConfigIntent(appWidgetId: Int): Intent = Intent(this, WidgetConfigActivity::class.java)
    .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    .setData(Uri.parse("journal://widget/$appWidgetId"))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
