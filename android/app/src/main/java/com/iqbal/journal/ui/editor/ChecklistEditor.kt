package com.iqbal.journal.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.ChecklistItem
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

/** Isi catatan mode checklist di Editor; anggota tanpa akses edit hanya melihat. */
@Composable
internal fun ChecklistEditor(
    items: List<ChecklistItem>,
    canEdit: Boolean,
    mutedColor: Color,
    onTextChange: (index: Int, text: String) -> Unit,
    onCheckedChange: (index: Int, checked: Boolean) -> Unit,
    onAdd: () -> Unit,
    onInsert: (index: Int, text: String, newItems: List<String>) -> Unit,
    onRemove: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Item yang baru disisipkan di tengah; komposisi baris berbasis posisi, jadi fokusnya diminta manual.
    var focusIndex by remember { mutableIntStateOf(NO_FOCUS) }
    Column(modifier.verticalScroll(rememberScrollState())) {
        items.forEachIndexed { index, item ->
            ChecklistRow(
                item = item,
                canEdit = canEdit,
                // Item kosong terakhir = baru ditambahkan, langsung siap diketik.
                autoFocus = canEdit && index == items.lastIndex && item.text.isEmpty(),
                focusNow = focusIndex == index,
                onFocused = { focusIndex = NO_FOCUS },
                mutedColor = mutedColor,
                onTextChange = { text ->
                    if ('\n' in text) {
                        val (current, newItems) = splitLines(old = item.text, new = text)
                        onInsert(index, current, newItems)
                        focusIndex = index + newItems.size
                    } else {
                        onTextChange(index, text)
                    }
                },
                onCheckedChange = { onCheckedChange(index, it) },
                onRemove = { onRemove(index) }
            )
        }
        if (canEdit) {
            TextButton(onClick = onAdd) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, Modifier.size(Sizes.progress))
                Spacer(Modifier.size(Spacing.small))
                Text(stringResource(R.string.editor_checklist_add))
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    canEdit: Boolean,
    autoFocus: Boolean,
    focusNow: Boolean,
    onFocused: () -> Unit,
    mutedColor: Color,
    onTextChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) focusRequester.requestFocus() }
    LaunchedEffect(focusNow) {
        if (focusNow) {
            focusRequester.requestFocus()
            onFocused()
        }
    }
    val style = MaterialTheme.typography.titleLarge
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = onCheckedChange,
            enabled = canEdit,
            modifier = Modifier.semantics { contentDescription = item.text }
        )
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            readOnly = !canEdit,
            textStyle = style.copy(
                color = if (item.checked) mutedColor else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { innerTextField ->
                if (item.text.isEmpty()) {
                    Text(stringResource(R.string.editor_checklist_placeholder), style = style, color = mutedColor)
                }
                innerTextField()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        if (canEdit) {
            IconButton(onClick = onRemove) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.editor_checklist_remove),
                    tint = mutedColor
                )
            }
        }
    }
}

private const val NO_FOCUS = -1

/**
 * Teks baru berisi baris baru: satu Enter menyisipkan item kosong di bawah tanpa memotong teks;
 * tempelan banyak baris menjadi satu item per baris (baris kosong dibuang).
 */
internal fun splitLines(old: String, new: String): Pair<String, List<String>> {
    if (new.replaceFirst("\n", "") == old) return old to listOf("")
    val lines = new.lines()
    return lines.first() to lines.drop(1).filter { it.isNotBlank() }
}
