package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.NoteAccents
import com.example.ui.theme.neumorphicRaised

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    noteId: String?,
    template: String? = null,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.load(noteId, template) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var titleField by remember { mutableStateOf(TextFieldValue()) }
    var contentField by remember { mutableStateOf(TextFieldValue()) }
    var showColorSheet by remember { mutableStateOf(false) }

    // Seed the editable fields once the note has been loaded / created.
    LaunchedEffect(state.id) {
        titleField = TextFieldValue(state.title, TextRange(state.title.length))
        contentField = TextFieldValue(state.content, TextRange(state.content.length))
    }

    fun leave() {
        viewModel.flush()
        onNavigateBack()
    }

    BackHandler { leave() }

    fun editContent(transform: (TextFieldValue) -> TextFieldValue) {
        val updated = transform(contentField)
        contentField = updated
        viewModel.onContentChanged(updated.text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        EditorTopBar(
            saveStatus = state.saveStatus,
            isPinned = state.isPinned,
            isFavorite = state.isFavorite,
            onBack = { leave() },
            onTogglePin = viewModel::togglePin,
            onToggleFavorite = viewModel::toggleFavorite,
            onColor = { showColorSheet = true },
            onDelete = { viewModel.deleteToTrash { onNavigateBack() } },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = titleField,
                onValueChange = {
                    titleField = it
                    viewModel.onTitleChanged(it.text)
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (titleField.text.isEmpty()) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))
            EditorMeta(
                words = state.wordCount,
                readingMinutes = state.readingMinutes,
            )
            Spacer(Modifier.height(16.dp))

            BasicTextField(
                value = contentField,
                onValueChange = {
                    contentField = it
                    viewModel.onContentChanged(it.text)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (contentField.text.isEmpty()) {
                        Text(
                            text = "Start writing your thoughts…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp),
            )
            Spacer(Modifier.height(24.dp))
        }

        FormattingToolbar(
            onHeader = { editContent { prefixLine(it, "# ") } },
            onBold = { editContent { wrapSelection(it, "**") } },
            onItalic = { editContent { wrapSelection(it, "*") } },
            onChecklist = { editContent { prefixLine(it, "- [ ] ") } },
            onBullet = { editContent { prefixLine(it, "- ") } },
            onNumbered = { editContent { prefixLine(it, "1. ") } },
            onQuote = { editContent { prefixLine(it, "> ") } },
            onCode = { editContent { wrapSelection(it, "`") } },
            onDivider = { editContent { insert(it, "\n\n---\n\n") } },
        )
    }

    if (showColorSheet) {
        ColorPickerSheet(
            selected = state.colorArgb,
            onSelect = {
                viewModel.setColor(it)
                showColorSheet = false
            },
            onDismiss = { showColorSheet = false },
        )
    }
}

@Composable
private fun EditorTopBar(
    saveStatus: SaveStatus,
    isPinned: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onColor: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeuIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            size = 44.dp,
        )
        Spacer(Modifier.width(12.dp))
        SaveStatusPill(saveStatus)
        Spacer(Modifier.weight(1f))
        NeuIconButton(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = "Favorite",
            onClick = onToggleFavorite,
            size = 44.dp,
            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        NeuIconButton(
            icon = Icons.Rounded.PushPin,
            contentDescription = "Pin",
            onClick = onTogglePin,
            size = 44.dp,
            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        NeuIconButton(
            icon = Icons.Rounded.Palette,
            contentDescription = "Note colour",
            onClick = onColor,
            size = 44.dp,
        )
        Spacer(Modifier.width(10.dp))
        NeuIconButton(
            icon = Icons.Rounded.Delete,
            contentDescription = "Move to Trash",
            onClick = onDelete,
            size = 44.dp,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SaveStatusPill(status: SaveStatus) {
    val (label, color) = when (status) {
        SaveStatus.Idle -> "" to MaterialTheme.colorScheme.onSurfaceVariant
        SaveStatus.Editing -> "Editing…" to MaterialTheme.colorScheme.onSurfaceVariant
        SaveStatus.Saving -> "Saving…" to MaterialTheme.colorScheme.onSurfaceVariant
        SaveStatus.Saved -> "Saved" to MaterialTheme.colorScheme.tertiary
    }
    AnimatedContent(targetState = label, label = "saveStatus") { text ->
        if (text.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status == SaveStatus.Saved) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun EditorMeta(words: Int, readingMinutes: Int) {
    Text(
        text = buildString {
            append(if (words == 1) "1 word" else "$words words")
            if (readingMinutes > 0) append("  ·  $readingMinutes min read")
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FormattingToolbar(
    onHeader: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onChecklist: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onQuote: () -> Unit,
    onCode: () -> Unit,
    onDivider: () -> Unit,
) {
    val neu = LocalNeuColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .neumorphicRaised(24.dp, neu, elevation = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToolbarButton(Icons.Rounded.Title, "Heading", onHeader)
        ToolbarButton(Icons.Rounded.FormatBold, "Bold", onBold)
        ToolbarButton(Icons.Rounded.FormatItalic, "Italic", onItalic)
        ToolbarButton(Icons.Rounded.Checklist, "Checklist", onChecklist)
        ToolbarButton(Icons.AutoMirrored.Rounded.FormatListBulleted, "Bullet list", onBullet)
        ToolbarButton(Icons.Rounded.FormatListNumbered, "Numbered list", onNumbered)
        ToolbarButton(Icons.Rounded.FormatQuote, "Quote", onQuote)
        ToolbarButton(Icons.Rounded.Code, "Code", onCode)
        ToolbarButton(Icons.Rounded.HorizontalRule, "Divider", onDivider)
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerSheet(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Note colour",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ColorSwatch(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    selected = selected == 0,
                    isNone = true,
                    onClick = { onSelect(0) },
                )
                NoteAccents.forEach { accent ->
                    val argb = accent.toArgb()
                    ColorSwatch(
                        color = accent,
                        selected = selected == argb,
                        onClick = { onSelect(argb) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    isNone: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = if (isNone) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ---- Markdown insertion helpers -----------------------------------------------

private fun wrapSelection(value: TextFieldValue, token: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val text = value.text
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + token + selected + token + text.substring(end)
    val cursor = start + token.length + selected.length + token.length
    return TextFieldValue(newText, TextRange(cursor))
}

private fun prefixLine(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val cursor = value.selection.min
    val searchFrom = (cursor - 1).coerceAtLeast(0)
    val lineStart = text.lastIndexOf('\n', searchFrom).let { if (it == -1) 0 else it + 1 }
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return TextFieldValue(newText, TextRange(cursor + prefix.length))
}

private fun insert(value: TextFieldValue, snippet: String): TextFieldValue {
    val start = value.selection.min
    val text = value.text
    val newText = text.substring(0, start) + snippet + text.substring(value.selection.max)
    return TextFieldValue(newText, TextRange(start + snippet.length))
}
