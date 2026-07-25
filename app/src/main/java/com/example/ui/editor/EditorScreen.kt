package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.attachments.AttachmentStore
import com.example.domain.model.Checklist as ChecklistUtil
import com.example.domain.model.ChecklistItem
import com.example.domain.model.NoteType
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.NoteAccents
import com.example.ui.theme.neumorphicRaised
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    var showImagePicker by remember { mutableStateOf(false) }
    val checklistItems = remember { mutableStateListOf<UiChecklistItem>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                AttachmentStore.importFromUri(context, uri)?.let { viewModel.addAttachment(it) }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) viewModel.addAttachment(file.name) else file?.delete()
        pendingCameraFile = null
    }

    // Seed the editable fields once the note has been loaded / created.
    LaunchedEffect(state.id) {
        titleField = TextFieldValue(state.title, TextRange(state.title.length))
        contentField = TextFieldValue(state.content, TextRange(state.content.length))
        if (state.type == NoteType.CHECKLIST) {
            checklistItems.clear()
            ChecklistUtil.parse(state.content).forEach {
                checklistItems.add(UiChecklistItem(UUID.randomUUID().toString(), it.text, it.checked))
            }
            if (checklistItems.isEmpty()) {
                checklistItems.add(UiChecklistItem(UUID.randomUUID().toString(), "", false))
            }
        }
    }

    fun pushChecklist() {
        viewModel.onContentChanged(
            ChecklistUtil.serialize(checklistItems.map { ChecklistItem(it.text, it.checked) })
        )
    }

    val focusManager = LocalFocusManager.current
    fun leave() {
        focusManager.clearFocus(force = true)
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
            onDelete = {
                focusManager.clearFocus(force = true)
                viewModel.deleteToTrash { onNavigateBack() }
            },
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
            if (state.type == NoteType.CHECKLIST) {
                ChecklistProgress(
                    done = checklistItems.count { it.checked && it.text.isNotBlank() },
                    total = checklistItems.count { it.text.isNotBlank() },
                )
            } else {
                EditorMeta(
                    words = state.wordCount,
                    readingMinutes = state.readingMinutes,
                )
            }

            Spacer(Modifier.height(16.dp))
            AttachmentsSection(
                attachments = state.attachments,
                onAdd = { showImagePicker = true },
                onRemove = { name ->
                    AttachmentStore.delete(context, name)
                    viewModel.removeAttachment(name)
                },
            )

            if (state.type == NoteType.CHECKLIST) {
                ChecklistBody(
                    items = checklistItems,
                    onChanged = { pushChecklist() },
                )
            } else {
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
            }
            Spacer(Modifier.height(24.dp))
        }

        if (state.type != NoteType.CHECKLIST) {
            FormattingToolbar(
                onHeader = { editContent { prefixLine(it, "# ") } },
                onBold = { editContent { wrapSelection(it, "**") } },
                onItalic = { editContent { wrapSelection(it, "*") } },
                onChecklist = { editContent { prefixLine(it, "- [ ] ") } },
                onBullet = { editContent { prefixLine(it, "- ") } },
                onNumbered = { editContent { prefixLine(it, "1. ") } },
                onQuote = { editContent { surround(it, "\"", "\"") } },
                onCode = { editContent { surround(it, "[", "]") } },
                onDivider = { editContent { insert(it, "\n\n---\n\n") } },
            )
        }
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

    if (showImagePicker) {
        ImagePickerSheet(
            onGallery = {
                showImagePicker = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onCamera = {
                showImagePicker = false
                val file = AttachmentStore.newImageFile(context)
                pendingCameraFile = file
                runCatching { cameraLauncher.launch(AttachmentStore.uriForFile(context, file)) }
                    .onFailure {
                        file.delete()
                        pendingCameraFile = null
                    }
            },
            onDismiss = { showImagePicker = false },
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
        ToolbarButton(Icons.Rounded.FormatQuote, "Quotes", onQuote)
        ToolbarButton(Icons.Rounded.Code, "Brackets", onCode)
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

// ---- Image attachments ---------------------------------------------------------

@Composable
private fun AttachmentsSection(
    attachments: List<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        attachments.forEach { name ->
            key(name) {
                AttachmentImage(
                    file = AttachmentStore.fileFor(context, name),
                    onRemove = { onRemove(name) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAdd)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = "Add image",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Add image",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AttachmentImage(file: File, onRemove: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = file,
            contentDescription = "Attached image",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove image",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "Add image",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            PickerRow(Icons.Rounded.PhotoLibrary, "Choose from gallery", onGallery)
            PickerRow(Icons.Rounded.PhotoCamera, "Take a photo", onCamera)
        }
    }
}

@Composable
private fun PickerRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---- Interactive checklist -----------------------------------------------------

private data class UiChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean,
)

@Composable
private fun ChecklistProgress(done: Int, total: Int) {
    Text(
        text = when {
            total == 0 -> "No items yet"
            done == total -> "All $total done 🎉"
            else -> "$done of $total done"
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChecklistBody(
    items: SnapshotStateList<UiChecklistItem>,
    onChanged: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            key(item.id) {
                ChecklistRow(
                    item = item,
                    onToggle = {
                        val i = items.indexOfFirst { it.id == item.id }
                        if (i >= 0) {
                            items[i] = items[i].copy(checked = !items[i].checked)
                            onChanged()
                        }
                    },
                    onTextChange = { text ->
                        val i = items.indexOfFirst { it.id == item.id }
                        if (i >= 0) {
                            items[i] = items[i].copy(text = text)
                            onChanged()
                        }
                    },
                    onDelete = {
                        items.removeAll { it.id == item.id }
                        if (items.isEmpty()) {
                            items.add(UiChecklistItem(UUID.randomUUID().toString(), "", false))
                        }
                        onChanged()
                    },
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    items.add(UiChecklistItem(UUID.randomUUID().toString(), "", false))
                    onChanged()
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add item",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Add item",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ChecklistRow(
    item: UiChecklistItem,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (item.checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (item.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(7.dp),
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (item.checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onBackground,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (item.text.isEmpty()) {
                    Text(
                        text = "List item",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                inner()
            },
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove item",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
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

private fun surround(value: TextFieldValue, open: String, close: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val text = value.text
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + open + selected + close + text.substring(end)
    val cursor = start + open.length + selected.length + close.length
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
