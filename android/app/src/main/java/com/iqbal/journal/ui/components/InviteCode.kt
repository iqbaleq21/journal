package com.iqbal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.iqbal.journal.domain.validation.GroupRules
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

/** Input kode undangan yang tampil sebagai kotak per karakter. */
@Composable
fun InviteCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(GroupRules.normalizeInviteCode(it)) },
        enabled = enabled,
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.semantics { contentDescription = label },
        decorationBox = { _ ->
            CodeBoxes(
                code = value,
                activeIndex = value.length.takeIf { enabled },
                filledBorder = MaterialTheme.colorScheme.primary,
                emptyBorder = MaterialTheme.colorScheme.outline,
                container = MaterialTheme.colorScheme.background
            )
        }
    )
}

/** Kode undangan read-only, untuk dibagikan. */
@Composable
fun InviteCodeDisplay(code: String, modifier: Modifier = Modifier) {
    CodeBoxes(
        code = code,
        activeIndex = null,
        filledBorder = Color.Transparent,
        emptyBorder = Color.Transparent,
        container = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = code }
    )
}

@Composable
private fun CodeBoxes(
    code: String,
    activeIndex: Int?,
    filledBorder: Color,
    emptyBorder: Color,
    container: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        repeat(GroupRules.INVITE_CODE_LENGTH) { index ->
            val char = code.getOrNull(index)
            val isActive = index == activeIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(Sizes.inputHeight)
                    .background(container, MaterialTheme.shapes.small)
                    .border(
                        width = if (char != null || isActive) Sizes.outlineThick else Sizes.outlineThin,
                        color = when {
                            char != null -> filledBorder
                            isActive -> MaterialTheme.colorScheme.onSurface
                            else -> emptyBorder
                        },
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Text(text = char?.toString().orEmpty(), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InviteCodeFieldPreview() {
    JournalTheme { InviteCodeField(value = "K7Q", onValueChange = {}, label = "Kode", onDone = {}) }
}
