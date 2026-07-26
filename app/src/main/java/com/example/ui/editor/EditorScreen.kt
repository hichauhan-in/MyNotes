package com.example.ui.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.StrikethroughS
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.attachments.AttachmentStore
import com.example.data.export.ExportFormat
import com.example.data.export.ExportIO
import com.example.data.export.Exporter
import com.example.domain.model.AttachmentKind
import com.example.domain.model.AttachmentMarkup
import com.example.domain.model.Checklist as ChecklistUtil
import com.example.domain.model.ChecklistItem
import com.example.domain.model.Note
import com.example.domain.model.NoteType
import com.example.ui.components.NeuIconButton
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.TemplateIcons
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.NoteAccents
import com.example.ui.theme.neumorphicRaised
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** When true, the editor is shown read-only; the pencil in the top bar toggles editing. */
internal val LocalReadOnly = staticCompositionLocalOf { false }

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    noteId: String?,
    template: String? = null,
    folderId: String? = null,
    templateId: String? = null,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.load(noteId, template, folderId, templateId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultExportFolder by viewModel.defaultExportFolder.collectAsStateWithLifecycle()

    var titleField by remember { mutableStateOf(TextFieldValue()) }
    // Existing notes open read-only; new notes and template drafts open ready to edit.
    var editing by remember { mutableStateOf(noteId == null) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showVoiceRecorder by remember { mutableStateOf(false) }
    var showMicRationale by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
    val checklistItems = remember { mutableStateListOf<UiChecklistItem>() }
    val blocks = remember { mutableStateListOf<EditorBlock>() }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var cropImageBlock by remember { mutableStateOf<ImageBlock?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    fun pushBlocks(immediate: Boolean) {
        val text = serializeBlocks(blocks)
        if (immediate) viewModel.commitContentNow(text) else viewModel.onContentChanged(text)
    }

    fun onTextBlockChange(id: String, value: TextFieldValue) {
        val idx = blocks.indexOfFirst { it.id == id }
        if (idx >= 0 && blocks[idx] is TextBlock) {
            blocks[idx] = TextBlock(id, value)
            pushBlocks(immediate = false)
        }
    }

    fun editFocusedBlock(transform: (TextFieldValue) -> TextFieldValue) {
        val focused = blocks.indexOfFirst { it.id == focusedBlockId }
        val idx = if (focused >= 0 && blocks[focused] is TextBlock) focused
        else blocks.indexOfLast { it is TextBlock }
        if (idx >= 0) {
            val tb = blocks[idx] as TextBlock
            blocks[idx] = TextBlock(tb.id, transform(tb.value))
            pushBlocks(immediate = false)
        }
    }

    // Insert a media block (image / audio) at the cursor, splitting the focused text block.
    fun insertBlockAtCursor(media: EditorBlock) {
        val idx = blocks.indexOfFirst { it.id == focusedBlockId }
        if (idx >= 0 && blocks[idx] is TextBlock) {
            val tb = blocks[idx] as TextBlock
            val cursor = tb.value.selection.min.coerceIn(0, tb.value.text.length)
            val before = tb.value.text.substring(0, cursor)
            val after = tb.value.text.substring(cursor)
            blocks[idx] = TextBlock(tb.id, TextFieldValue(before, TextRange(before.length)))
            blocks.add(idx + 1, media)
            blocks.add(idx + 2, TextBlock(newBlockId(), TextFieldValue(after)))
        } else {
            blocks.add(media)
            blocks.add(TextBlock(newBlockId(), TextFieldValue("")))
        }
        pushBlocks(immediate = true)
    }

    fun insertImageAtCursor(fileName: String) = insertBlockAtCursor(ImageBlock(newBlockId(), fileName))

    fun insertAudioAtCursor(fileName: String) = insertBlockAtCursor(AudioBlock(newBlockId(), fileName))

    fun resizeImageBlock(blockId: String, widthPercent: Int) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0 && blocks[idx] is ImageBlock) {
            blocks[idx] = (blocks[idx] as ImageBlock).copy(widthPercent = widthPercent)
            pushBlocks(immediate = true)
        }
    }

    // Swap an image block's file for a freshly cropped one and drop the old file.
    fun replaceImageFile(blockId: String, newFileName: String) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0 && blocks[idx] is ImageBlock) {
            val old = blocks[idx] as ImageBlock
            if (old.fileName != newFileName) {
                blocks[idx] = old.copy(fileName = newFileName)
                AttachmentStore.delete(context, old.fileName)
                pushBlocks(immediate = true)
            }
        }
    }

    fun removeBlock(blockId: String) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return
        val fileName = when (val b = blocks[idx]) {
            is ImageBlock -> b.fileName
            is AudioBlock -> b.fileName
            else -> null
        }
        blocks.removeAt(idx)
        // Merge the text blocks that surrounded the media so the cursor flows naturally.
        if (idx - 1 >= 0 && idx < blocks.size && blocks[idx - 1] is TextBlock && blocks[idx] is TextBlock) {
            val a = blocks[idx - 1] as TextBlock
            val b = blocks[idx] as TextBlock
            val merged = when {
                a.value.text.isEmpty() -> b.value.text
                b.value.text.isEmpty() -> a.value.text
                else -> a.value.text + "\n" + b.value.text
            }
            blocks[idx - 1] = TextBlock(a.id, TextFieldValue(merged, TextRange(a.value.text.length)))
            blocks.removeAt(idx)
        }
        fileName?.let { AttachmentStore.delete(context, it) }
        pushBlocks(immediate = true)
    }

    fun insertChecklistBlock() =
        insertBlockAtCursor(ChecklistBlock(newBlockId(), listOf(ChecklistEntry(newBlockId(), "", false))))

    fun updateChecklistBlock(blockId: String, items: List<ChecklistEntry>) {
        if (items.isEmpty()) {
            removeBlock(blockId)
            return
        }
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0 && blocks[idx] is ChecklistBlock) {
            blocks[idx] = ChecklistBlock(blockId, items)
            pushBlocks(immediate = false)
        }
    }

    fun updateTableBlock(blockId: String, newBlock: TableBlock, immediate: Boolean) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0 && blocks[idx] is TableBlock) {
            blocks[idx] = newBlock
            pushBlocks(immediate = immediate)
        }
    }

    fun insertCalloutBlock() = insertBlockAtCursor(CalloutBlock(newBlockId(), "💡", ""))

    fun updateCalloutBlock(blockId: String, newBlock: CalloutBlock) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0 && blocks[idx] is CalloutBlock) {
            blocks[idx] = newBlock
            pushBlocks(immediate = false)
        }
    }

    fun insertScribbleBlock() = insertBlockAtCursor(ScribbleBlock(newBlockId(), emptyList(), 220))

    fun updateScribbleBlock(blockId: String, newBlock: ScribbleBlock, immediate: Boolean) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0 && blocks[idx] is ScribbleBlock) {
            blocks[idx] = newBlock
            pushBlocks(immediate = immediate)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) showVoiceRecorder = true else showMicRationale = true }

    fun startVoiceNote() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) showVoiceRecorder = true
        else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val name = withContext(Dispatchers.IO) { AttachmentStore.importFromUri(context, uri) }
                if (name != null) insertImageAtCursor(name)
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) insertImageAtCursor(file.name) else file?.delete()
        pendingCameraFile = null
    }

    fun buildExportNote() = Note(
        id = state.id,
        title = state.title,
        content = state.content,
        createdAt = state.createdAt,
        updatedAt = state.updatedAt,
        isPinned = state.isPinned,
        isFavorite = state.isFavorite,
        folderId = state.folderId,
        tags = state.tags,
        colorArgb = state.colorArgb,
        type = state.type,
        attachments = state.attachments,
    )

    // Notes with images / voice notes export as a self-contained ZIP (note file + attachments);
    // plain notes export as the single chosen file.
    fun writeNoteExport(uri: android.net.Uri, note: Note, format: ExportFormat): Boolean =
        if (note.attachments.isNotEmpty()) {
            ExportIO.writeStream(context, uri) { out -> Exporter.writeNoteZip(context, note, format, out) }
        } else {
            ExportIO.writeBytes(context, uri, Exporter.noteBytes(note, format))
        }

    val exportDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val format = pendingExportFormat
        pendingExportFormat = null
        if (uri != null && format != null) {
            val note = buildExportNote()
            scope.launch {
                val ok = withContext(Dispatchers.IO) { writeNoteExport(uri, note, format) }
                android.widget.Toast.makeText(
                    context,
                    if (ok) "Exported" else "Couldn't export",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun exportNote(format: ExportFormat) {
        val note = buildExportNote()
        val zipped = note.attachments.isNotEmpty()
        val ext = if (zipped) "zip" else format.ext
        val mime = if (zipped) "application/zip" else format.mime
        val fileName = "${Exporter.noteFileBase(note)}.$ext"
        val folder = defaultExportFolder
        if (folder != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    val target = ExportIO.createInTree(context, folder, fileName, mime)
                    target != null && writeNoteExport(target, note, format)
                }
                if (ok) {
                    android.widget.Toast.makeText(context, "Exported to your folder", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    // Saved folder unavailable - fall back to the picker.
                    pendingExportFormat = format
                    runCatching { exportDocLauncher.launch(fileName) }
                }
            }
        } else {
            pendingExportFormat = format
            runCatching { exportDocLauncher.launch(fileName) }
        }
    }

    // Seed the editable fields once the note has been loaded / created.
    LaunchedEffect(state.id) {
        titleField = TextFieldValue(state.title, TextRange(state.title.length))
        if (state.type == NoteType.CHECKLIST) {
            checklistItems.clear()
            ChecklistUtil.parse(state.content).forEach {
                checklistItems.add(UiChecklistItem(UUID.randomUUID().toString(), it.text, it.checked))
            }
            if (checklistItems.isEmpty()) {
                checklistItems.add(UiChecklistItem(UUID.randomUUID().toString(), "", false))
            }
        } else if (state.type != NoteType.SHEET && state.type != NoteType.EXPENSE && state.type != NoteType.SCRIBBLE) {
            val parsed = parseContentToBlocks(state.content).toMutableList()
            // Migrate legacy notes whose images were kept only in `attachments`.
            if (parsed.none { it is ImageBlock } && state.attachments.isNotEmpty()) {
                state.attachments.forEach { name ->
                    parsed.add(ImageBlock(newBlockId(), name))
                    parsed.add(TextBlock(newBlockId(), TextFieldValue("")))
                }
            }
            blocks.clear()
            blocks.addAll(parsed)
            focusedBlockId = parsed.firstOrNull { it is TextBlock }?.id
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        EditorTopBar(
            saveStatus = state.saveStatus,
            templateMode = state.templateMode,
            editing = editing,
            onBack = { leave() },
            onToggleEdit = {
                if (editing) {
                    focusManager.clearFocus(force = true)
                    viewModel.flush()
                }
                editing = !editing
            },
            onShare = { showShareSheet = true },
            onExport = { showExportSheet = true },
            onDelete = {
                focusManager.clearFocus(force = true)
                viewModel.deleteToTrash { onNavigateBack() }
            },
        )

        CompositionLocalProvider(LocalReadOnly provides !editing) {
        val metaBar: @Composable () -> Unit = {
            if (editing) {
                EditorActionRow(
                    isFavorite = state.isFavorite,
                    isPinned = state.isPinned,
                    colorArgb = state.colorArgb,
                    tags = state.tags,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onTogglePin = viewModel::togglePin,
                    onColor = { showColorSheet = true },
                    onEditTags = { showTagSheet = true },
                )
            }
        }

        when (state.type) {
            NoteType.SHEET -> SheetEditor(
                seedKey = state.id,
                title = state.title,
                content = state.content,
                onTitleChange = viewModel::onTitleChanged,
                onContentChange = viewModel::onContentChanged,
                meta = metaBar,
                modifier = Modifier.weight(1f),
            )

            NoteType.EXPENSE -> ExpenseEditor(
                seedKey = state.id,
                title = state.title,
                content = state.content,
                onTitleChange = viewModel::onTitleChanged,
                onContentChange = viewModel::onContentChanged,
                meta = metaBar,
                modifier = Modifier.weight(1f),
            )

            NoteType.SCRIBBLE -> ScribbleEditor(
                seedKey = state.id,
                title = state.title,
                content = state.content,
                onTitleChange = viewModel::onTitleChanged,
                onContentChange = viewModel::onContentChanged,
                meta = metaBar,
                modifier = Modifier.weight(1f),
            )

            else -> {
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
                readOnly = LocalReadOnly.current,
                decorationBox = { inner ->
                    if (titleField.text.isEmpty()) {
                        Text(
                            text = if (state.templateMode) "Template name" else "Title",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))
            if (state.templateMode) {
                TemplateIconRow(
                    selected = state.iconKey,
                    onSelect = viewModel::setTemplateIcon,
                )
            } else {
                metaBar()
                Spacer(Modifier.height(12.dp))
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
            }

            Spacer(Modifier.height(16.dp))
            if (state.type == NoteType.CHECKLIST) {
                ChecklistBody(
                    items = checklistItems,
                    onChanged = { pushChecklist() },
                )
            } else {
                blocks.forEachIndexed { index, block ->
                    key(block.id) {
                        when (block) {
                            is TextBlock -> EditorTextBlock(
                                value = block.value,
                                showHint = blocks.size == 1 && block.value.text.isEmpty(),
                                isLast = index == blocks.lastIndex,
                                onValueChange = { onTextBlockChange(block.id, it) },
                                onFocused = { focusedBlockId = block.id },
                            )

                            is ImageBlock -> {
                                ResizableAttachmentImage(
                                    file = AttachmentStore.fileFor(context, block.fileName),
                                    widthFraction = (block.widthPercent ?: 100) / 100f,
                                    onWidthChange = { pct -> resizeImageBlock(block.id, pct) },
                                    onRemove = { removeBlock(block.id) },
                                    onCrop = { cropImageBlock = block },
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            is AudioBlock -> {
                                AudioAttachment(
                                    file = AttachmentStore.fileFor(context, block.fileName),
                                    onRemove = { removeBlock(block.id) },
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            is ChecklistBlock -> {
                                ChecklistBlockView(
                                    block = block,
                                    onChange = { items -> updateChecklistBlock(block.id, items) },
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            is TableBlock -> {
                                TableBlockView(
                                    block = block,
                                    onChange = { updated, immediate ->
                                        updateTableBlock(block.id, updated, immediate)
                                    },
                                    onRemove = { removeBlock(block.id) },
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            is CalloutBlock -> {
                                CalloutBlockView(
                                    block = block,
                                    onChange = { updated -> updateCalloutBlock(block.id, updated) },
                                    onRemove = { removeBlock(block.id) },
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            is ScribbleBlock -> {
                                ScribbleBlockView(
                                    block = block,
                                    onChange = { updated, immediate ->
                                        updateScribbleBlock(block.id, updated, immediate)
                                    },
                                    onRemove = { removeBlock(block.id) },
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (state.templateMode) {
            SaveTemplateBar(
                enabled = state.title.isNotBlank(),
                onSave = {
                    focusManager.clearFocus(force = true)
                    viewModel.saveTemplate { onNavigateBack() }
                },
            )
        }

        if (editing && state.type != NoteType.CHECKLIST) {
            FormattingToolbar(
                onImage = { showImagePicker = true },
                onVoice = { startVoiceNote() },
                onScribble = { insertScribbleBlock() },
                onHeading = { prefix -> editFocusedBlock { prefixLine(it, prefix) } },
                onFormat = { token -> editFocusedBlock { wrapSelection(it, token) } },
                onBullet = { editFocusedBlock { prefixLine(it, "- ") } },
                onNumbered = { editFocusedBlock { prefixLine(it, "1. ") } },
                onChecklist = { insertChecklistBlock() },
                onWrap = { open, close -> editFocusedBlock { surround(it, open, close) } },
                onDivider = { editFocusedBlock { insert(it, "\n\n--------------------\n\n") } },
                onTable = { showTableDialog = true },
                onCallout = { insertCalloutBlock() },
            )
        }
        }
        }
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

    if (showShareSheet) {
        ShareOptionsSheet(onDismiss = { showShareSheet = false })
    }

    if (showExportSheet) {
        ExportOptionsSheet(
            onPick = { format ->
                showExportSheet = false
                exportNote(format)
            },
            onDismiss = { showExportSheet = false },
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

    if (showVoiceRecorder) {
        VoiceRecorderSheet(
            onSave = { fileName ->
                insertAudioAtCursor(fileName)
                showVoiceRecorder = false
            },
            onCancel = { showVoiceRecorder = false },
        )
    }

    if (showMicRationale) {
        MicPermissionDialog(
            onOpenSettings = {
                showMicRationale = false
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            },
            onDismiss = { showMicRationale = false },
        )
    }

    if (showTagSheet) {
        TagEditorSheet(
            tags = state.tags,
            onAdd = { viewModel.addTag(it) },
            onRemove = { viewModel.removeTag(it) },
            onDismiss = { showTagSheet = false },
        )
    }

    if (showTableDialog) {
        TableSizeDialog(
            onCreate = { rows, cols ->
                insertBlockAtCursor(emptyTable(rows, cols))
                showTableDialog = false
            },
            onDismiss = { showTableDialog = false },
        )
    }

    cropImageBlock?.let { imageBlock ->
        ImageCropDialog(
            file = AttachmentStore.fileFor(context, imageBlock.fileName),
            onCropped = { newName ->
                replaceImageFile(imageBlock.id, newName)
                cropImageBlock = null
            },
            onDismiss = { cropImageBlock = null },
        )
    }
}

@Composable
private fun EditorTopBar(
    saveStatus: SaveStatus,
    templateMode: Boolean,
    editing: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
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
        if (templateMode) {
            Text(
                text = "Template",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
        } else {
            SaveStatusPill(saveStatus)
            Spacer(Modifier.weight(1f))
            NeuIconButton(
                icon = if (editing) Icons.Rounded.Check else Icons.Rounded.Edit,
                contentDescription = if (editing) "Done editing" else "Edit",
                onClick = onToggleEdit,
                size = 44.dp,
                tint = if (editing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            EditorOverflowMenu(onShare = onShare, onExport = onExport, onDelete = onDelete)
        }
    }
}

/** The top-right overflow (⋮) menu: Share, Export and Move-to-Trash, each with its own icon. */
@Composable
private fun EditorOverflowMenu(
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        NeuIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = "More options",
            onClick = { open = true },
            size = 44.dp,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                onClick = { open = false; onShare() },
            )
            DropdownMenuItem(
                text = { Text("Export") },
                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                onClick = { open = false; onExport() },
            )
            DropdownMenuItem(
                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

/**
 * The shared "note controls" row shown just under the title on every note type: favourite, pin,
 * colour and tags. Tapping favourite/pin toggles them; colour and tags open their pickers.
 */
@Composable
private fun EditorActionRow(
    isFavorite: Boolean,
    isPinned: Boolean,
    colorArgb: Int,
    tags: List<String>,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onColor: () -> Unit,
    onEditTags: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetaChip(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = "Favourite",
            active = isFavorite,
            activeColor = MaterialTheme.colorScheme.secondary,
            onClick = onToggleFavorite,
        )
        MetaChip(
            icon = Icons.Rounded.PushPin,
            contentDescription = "Pin",
            active = isPinned,
            activeColor = MaterialTheme.colorScheme.primary,
            onClick = onTogglePin,
        )
        ColorChip(colorArgb = colorArgb, onClick = onColor)
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .height(22.dp)
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        )
        tags.forEach { tag ->
            Text(
                text = "#$tag",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onEditTags)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                .clickable(onClick = onEditTags)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Add tag",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MetaChip(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(if (active) Modifier.background(activeColor.copy(alpha = 0.14f)) else Modifier)
            .border(
                1.dp,
                if (active) activeColor else MaterialTheme.colorScheme.outlineVariant,
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ColorChip(colorArgb: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (colorArgb != 0) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(colorArgb)),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = "Note colour",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Horizontal icon picker shown when drafting a template. */
@Composable
private fun TemplateIconRow(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(
            text = "Icon",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TemplateIcons.options.forEach { (key, icon) ->
                val isSelected = key == selected
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable { onSelect(key) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** The always-visible "Save as template" action, sitting just above the formatting toolbar. */
@Composable
private fun SaveTemplateBar(enabled: Boolean, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        BrandGradientButton(
            text = "Save as template",
            onClick = { if (enabled) onSave() },
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f),
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

private enum class ListStyle(val icon: ImageVector, val label: String) {
    BULLET(Icons.AutoMirrored.Rounded.FormatListBulleted, "Bullet list"),
    NUMBERED(Icons.Rounded.FormatListNumbered, "Numbered list"),
    CHECKLIST(Icons.Rounded.Checklist, "Checklist"),
}

private enum class FormatStyle(val icon: ImageVector, val label: String, val token: String) {
    BOLD(Icons.Rounded.FormatBold, "Bold", "**"),
    ITALIC(Icons.Rounded.FormatItalic, "Italic", "*"),
    STRIKETHROUGH(Icons.Rounded.StrikethroughS, "Strikethrough", "~~"),
    HIGHLIGHT(Icons.Rounded.FormatColorFill, "Highlight", "=="),
}

private enum class HeadingStyle(val label: String, val prefix: String, val badge: String) {
    H1("Large title", "# ", "H1"),
    H2("Medium title", "## ", "H2"),
    H3("Small title", "### ", "H3"),
}

private data class WrapStyle(val glyph: String, val label: String, val open: String, val close: String)

private val wrapStyles = listOf(
    WrapStyle("\"", "Double quotes", "\"", "\""),
    WrapStyle("'", "Single quotes", "'", "'"),
    WrapStyle("(", "Parentheses", "(", ")"),
    WrapStyle("[", "Brackets", "[", "]"),
    WrapStyle("{", "Braces", "{", "}"),
    WrapStyle("`", "Code", "`", "`"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormattingToolbar(
    onImage: () -> Unit,
    onVoice: () -> Unit,
    onScribble: () -> Unit,
    onHeading: (prefix: String) -> Unit,
    onFormat: (token: String) -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onChecklist: () -> Unit,
    onWrap: (open: String, close: String) -> Unit,
    onDivider: () -> Unit,
    onTable: () -> Unit,
    onCallout: () -> Unit,
) {
    val neu = LocalNeuColors.current
    var listStyle by remember { mutableStateOf(ListStyle.BULLET) }
    var formatStyle by remember { mutableStateOf(FormatStyle.BOLD) }
    var headingStyle by remember { mutableStateOf(HeadingStyle.H1) }
    var wrapIndex by remember { mutableStateOf(0) }

    fun applyList(style: ListStyle) = when (style) {
        ListStyle.BULLET -> onBullet()
        ListStyle.NUMBERED -> onNumbered()
        ListStyle.CHECKLIST -> onChecklist()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .neumorphicRaised(24.dp, neu, elevation = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToolbarAccentButton(Icons.Rounded.AddPhotoAlternate, "Add image", onImage)
        Spacer(Modifier.width(6.dp))
        ToolbarAccentButton(Icons.Rounded.Mic, "Record voice note", onVoice)
        Spacer(Modifier.width(6.dp))
        ToolbarAccentButton(Icons.Rounded.Gesture, "Scribble", onScribble)
        ToolbarDivider()
        ToolbarMenuButton(
            description = headingStyle.label,
            onClick = { onHeading(headingStyle.prefix) },
            icon = Icons.Rounded.Title,
        ) { dismiss ->
            HeadingStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text("${style.badge}   ${style.label}") },
                    onClick = {
                        headingStyle = style
                        dismiss()
                        onHeading(style.prefix)
                    },
                )
            }
        }
        ToolbarMenuButton(
            description = formatStyle.label,
            onClick = { onFormat(formatStyle.token) },
            icon = formatStyle.icon,
        ) { dismiss ->
            FormatStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.label) },
                    leadingIcon = { Icon(style.icon, contentDescription = null) },
                    onClick = {
                        formatStyle = style
                        dismiss()
                        onFormat(style.token)
                    },
                )
            }
        }
        ToolbarMenuButton(
            description = listStyle.label,
            onClick = { applyList(listStyle) },
            icon = listStyle.icon,
        ) { dismiss ->
            ListStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.label) },
                    leadingIcon = { Icon(style.icon, contentDescription = null) },
                    onClick = {
                        listStyle = style
                        dismiss()
                        applyList(style)
                    },
                )
            }
        }
        val wrap = wrapStyles[wrapIndex]
        ToolbarMenuButton(
            description = wrap.label,
            onClick = { onWrap(wrap.open, wrap.close) },
            glyph = wrap.glyph,
        ) { dismiss ->
            wrapStyles.forEachIndexed { i, style ->
                DropdownMenuItem(
                    text = { Text("${style.label}   ${style.open}${style.close}") },
                    onClick = {
                        wrapIndex = i
                        dismiss()
                        onWrap(style.open, style.close)
                    },
                )
            }
        }
        ToolbarButton(Icons.Rounded.HorizontalRule, "Divider", onDivider)
        ToolbarButton(Icons.Rounded.TableChart, "Table", onTable)
        ToolbarButton(Icons.Rounded.Lightbulb, "Callout", onCallout)
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(26.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)),
    )
}

/** A toolbar button that applies its current style on tap and opens a chooser on long-press.
 *  The chosen style becomes the new default reflected on the button. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarMenuButton(
    description: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    glyph: String? = null,
    menu: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = { expanded = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = glyph ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menu { expanded = false }
        }
    }
}

/** The image / voice buttons are deliberately styled differently - raised, tinted tiles -
 *  so adding media reads as a primary action in the toolbar. */
@Composable
private fun ToolbarAccentButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val neu = LocalNeuColors.current
    Box(
        modifier = Modifier
            .size(42.dp)
            .neumorphicRaised(13.dp, neu, elevation = 6.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.primary)
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
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(22.dp),
        )
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

// ---- Share & export -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareOptionsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
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
                text = "Share",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choose how you'd like to share this note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SheetOptionRow(Icons.Rounded.Share, "Share as text", "Send the note's text to another app") {
                comingSoon(context); onDismiss()
            }
            SheetOptionRow(Icons.Rounded.PictureAsPdf, "Share as PDF", "Attach a PDF copy") {
                comingSoon(context); onDismiss()
            }
            SheetOptionRow(Icons.Rounded.Link, "Copy link", "A shareable link to this note") {
                comingSoon(context); onDismiss()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportOptionsSheet(onPick: (ExportFormat) -> Unit, onDismiss: () -> Unit) {
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
                text = "Export",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pick a format to save this note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SheetOptionRow(Icons.Rounded.PictureAsPdf, "PDF document", ".pdf") { onPick(ExportFormat.PDF) }
            SheetOptionRow(Icons.Rounded.Description, "Markdown", ".md") { onPick(ExportFormat.MD) }
            SheetOptionRow(Icons.Rounded.TextSnippet, "Plain text", ".txt") { onPick(ExportFormat.TXT) }
            SheetOptionRow(Icons.Rounded.Code, "Web page", ".html") { onPick(ExportFormat.HTML) }
        }
    }
}

@Composable
private fun SheetOptionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun comingSoon(context: android.content.Context) {
    android.widget.Toast.makeText(context, "Coming soon", android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
private fun MicPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Microphone access needed") },
        text = {
            Text(
                "To record a voice note, MyNotes+ needs permission to use the microphone. " +
                    "Allow it from the system prompt, or enable it in Settings if you previously declined.",
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}

// ---- Tags ----------------------------------------------------------------------

@Composable
private fun TagRow(tags: List<String>, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Text(
                text = "#$tag",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                .clickable(onClick = onEdit)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Add tag",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditorSheet(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val neu = LocalNeuColors.current
    val sheetState = rememberModalBottomSheetState()
    var input by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .imePadding(),
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add tags to group notes. Search a tag on the home screen to find them all.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .neumorphicRaised(16.dp, neu, elevation = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                ) {
                    if (input.isEmpty()) {
                        Text(
                            text = "New tag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it.replace("\n", "") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (input.isNotBlank()) {
                                onAdd(input)
                                input = ""
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add tag",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tags.forEach { tag ->
                        RemovableTagChip(text = "#$tag") { onRemove(tag) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemovableTagChip(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove tag",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ---- Inline editor blocks (text + images) -------------------------------------
@Composable
private fun EditorTextBlock(
    value: TextFieldValue,
    showHint: Boolean,
    isLast: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onFocused: () -> Unit,
) {
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    val transformation = remember(markerColor, highlightColor) {
        markdownVisualTransformation(markerColor, highlightColor)
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = transformation,
        readOnly = LocalReadOnly.current,
        decorationBox = { inner ->
            if (showHint) {
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
            .heightIn(min = if (isLast) 220.dp else 44.dp)
            .onFocusChanged { if (it.isFocused) onFocused() },
    )
}

/**
 * Renders markdown inline while keeping the raw text 1:1 (identity offset mapping, so the
 * cursor stays correct): **bold**, *italic* and `# heading` lines get real styling, and the
 * syntax markers are dimmed rather than hidden.
 */
private fun markdownVisualTransformation(markerColor: Color, highlightColor: Color): VisualTransformation =
    VisualTransformation { text ->
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)
        Regex("^(#{1,6})\\s.*$", RegexOption.MULTILINE).findAll(raw).forEach { m ->
            val level = m.groupValues[1].length
            val size = (24 - (level - 1) * 2).coerceAtLeast(15).sp
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontSize = size),
                m.range.first,
                m.range.last + 1,
            )
            builder.addStyle(
                SpanStyle(color = markerColor),
                m.range.first,
                (m.range.first + level + 1).coerceAtMost(raw.length),
            )
        }
        Regex("\\*\\*(.+?)\\*\\*").findAll(raw).forEach { m ->
            builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
            builder.addStyle(SpanStyle(color = markerColor, fontWeight = FontWeight.Normal), m.range.first, m.range.first + 2)
            builder.addStyle(SpanStyle(color = markerColor, fontWeight = FontWeight.Normal), m.range.last - 1, m.range.last + 1)
        }
        Regex("(?<![*\\w])\\*(?!\\s)([^*\\n]+?)\\*(?![*\\w])").findAll(raw).forEach { m ->
            builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1)
            builder.addStyle(SpanStyle(color = markerColor), m.range.first, m.range.first + 1)
            builder.addStyle(SpanStyle(color = markerColor), m.range.last, m.range.last + 1)
        }
        Regex("~~(.+?)~~").findAll(raw).forEach { m ->
            builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), m.range.first, m.range.last + 1)
            builder.addStyle(SpanStyle(color = markerColor), m.range.first, m.range.first + 2)
            builder.addStyle(SpanStyle(color = markerColor), m.range.last - 1, m.range.last + 1)
        }
        Regex("==(.+?)==").findAll(raw).forEach { m ->
            builder.addStyle(SpanStyle(background = highlightColor), m.range.first, m.range.last + 1)
            builder.addStyle(SpanStyle(color = markerColor), m.range.first, m.range.first + 2)
            builder.addStyle(SpanStyle(color = markerColor), m.range.last - 1, m.range.last + 1)
        }
        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

@Composable
private fun ResizableAttachmentImage(
    file: File,
    widthFraction: Float,
    onWidthChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onCrop: () -> Unit,
) {
    val context = LocalContext.current
    val readOnly = LocalReadOnly.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        // Local, smoothly-updated width while dragging; synced when the saved value changes.
        var fraction by remember { mutableStateOf(widthFraction) }
        LaunchedEffect(widthFraction) { fraction = widthFraction }
        // Decode once at the full container width so resizing just scales the cached bitmap
        // (no re-decode flicker mid-drag).
        val request = remember(file.path, maxWidthPx) {
            ImageRequest.Builder(context)
                .data(file)
                .size(maxWidthPx.roundToInt().coerceAtLeast(1))
                .build()
        }

        Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0.3f, 1f))) {
            AsyncImage(
                model = request,
                contentDescription = "Attached image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            if (!readOnly) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ImageOverlayButton(icon = Icons.Rounded.Crop, description = "Crop image", onClick = onCrop)
                    ImageOverlayButton(icon = Icons.Rounded.Close, description = "Remove image", onClick = onRemove)
                }
            }
            // Drag this corner handle to scale the image down (or back up).
            if (!readOnly) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                        .pointerInput(maxWidthPx) {
                            detectDragGestures(
                                onDrag = { change, drag ->
                                    change.consume()
                                    if (maxWidthPx > 0f) {
                                        fraction = (fraction + drag.x / maxWidthPx).coerceIn(0.3f, 1f)
                                    }
                                },
                                onDragEnd = { onWidthChange((fraction * 100).roundToInt()) },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = "Resize image",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageOverlayButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
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
    val readOnly = LocalReadOnly.current
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
                .clickable(enabled = !readOnly) {
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
    val readOnly = LocalReadOnly.current
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
                .clickable(enabled = !readOnly, onClick = onToggle),
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
            readOnly = readOnly,
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
                .clickable(enabled = !readOnly, onClick = onDelete),
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

// ---- Inline checklist & table blocks ------------------------------------------

@Composable
private fun ChecklistBlockView(
    block: ChecklistBlock,
    onChange: (List<ChecklistEntry>) -> Unit,
) {
    val neu = LocalNeuColors.current
    val readOnly = LocalReadOnly.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(16.dp, neu, elevation = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        block.items.forEach { item ->
            key(item.id) {
                ChecklistRow(
                    item = UiChecklistItem(item.id, item.text, item.checked),
                    onToggle = {
                        onChange(block.items.map { if (it.id == item.id) it.copy(checked = !it.checked) else it })
                    },
                    onTextChange = { text ->
                        onChange(block.items.map { if (it.id == item.id) it.copy(text = text) else it })
                    },
                    onDelete = {
                        onChange(block.items.filterNot { it.id == item.id })
                    },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !readOnly) { onChange(block.items + ChecklistEntry(newBlockId(), "", false)) }
                .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add item",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add item",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TableBlockView(
    block: TableBlock,
    onChange: (TableBlock, Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val neu = LocalNeuColors.current
    val current by rememberUpdatedState(block)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.TableChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.weight(1f))
            TableCtrl(Icons.Rounded.Remove, "Remove column") { onChange(current.removeColumn(), true) }
            TableCtrl(Icons.Rounded.Add, "Add column") { onChange(current.addColumn(), true) }
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(20.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)),
            )
            TableCtrl(Icons.Rounded.Remove, "Remove row") { onChange(current.removeRow(), true) }
            TableCtrl(Icons.Rounded.Add, "Add row") { onChange(current.addRow(), true) }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !LocalReadOnly.current, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove table",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .neumorphicRaised(12.dp, neu, elevation = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // Column headers (A, B, C …) with a drag-to-resize grip on each right edge.
            Row {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(28.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                block.columnWidths.forEachIndexed { c, w ->
                    TableColumnHeaderCell(
                        label = tableColumnLabel(c),
                        width = w,
                        onResize = { nw -> onChange(current.setColumnWidth(c, nw), false) },
                        onResizeEnd = { onChange(current, true) },
                    )
                }
            }
            // Data rows, each labelled with its 1-based row number.
            block.cells.forEachIndexed { r, row ->
                Row {
                    TableRowHeaderCell(number = r + 1)
                    row.forEachIndexed { c, cell ->
                        val w = block.columnWidths.getOrElse(c) { 130 }
                        Box(
                            modifier = Modifier
                                .width(w.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(8.dp),
                        ) {
                            BasicTextField(
                                value = cell,
                                onValueChange = { onChange(current.setCell(r, c, it), false) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                readOnly = LocalReadOnly.current,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tableColumnLabel(c: Int): String {
    var n = c
    val sb = StringBuilder()
    do {
        sb.insert(0, ('A' + (n % 26)))
        n = n / 26 - 1
    } while (n >= 0)
    return sb.toString()
}

@Composable
private fun TableColumnHeaderCell(
    label: String,
    width: Int,
    onResize: (Int) -> Unit,
    onResizeEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val currentWidth by rememberUpdatedState(width)
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(28.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(18.dp)
                .pointerInput(Unit) {
                    var startW = 0
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { startW = currentWidth; acc = 0f },
                        onDrag = { change, drag ->
                            change.consume()
                            acc += drag.x
                            val deltaDp = with(density) { acc.toDp().value }.toInt()
                            onResize((startW + deltaDp).coerceIn(60, 320))
                        },
                        onDragEnd = { onResizeEnd() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            TableGripDots()
        }
    }
}

@Composable
private fun TableRowHeaderCell(number: Int) {
    Box(
        modifier = Modifier
            .width(34.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Four dots hinting the column boundary can be dragged to resize. */
@Composable
private fun TableGripDots() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(2.5.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun TableCtrl(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = !LocalReadOnly.current, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A highlighted note box (tip / warning / reminder). Tap the badge to cycle its icon. */
@Composable
private fun CalloutBlockView(
    block: CalloutBlock,
    onChange: (CalloutBlock) -> Unit,
    onRemove: () -> Unit,
) {
    val emojis = remember { listOf("💡", "⚠️", "✅", "📌", "🔥", "❤️", "📝", "❓", "⭐", "🚀") }
    val readOnly = LocalReadOnly.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .clickable(enabled = !readOnly) {
                    val idx = emojis.indexOf(block.emoji).coerceAtLeast(0)
                    onChange(block.copy(emoji = emojis[(idx + 1) % emojis.size]))
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(block.emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = block.text,
            onValueChange = { onChange(block.copy(text = it)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            readOnly = readOnly,
            decorationBox = { inner ->
                if (block.text.isEmpty()) {
                    Text(
                        text = "Write a callout: tip, warning, reminder…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = !readOnly, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove callout",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** A lightweight, resizable freehand drawing area inside a note. */
@Composable
private fun ScribbleBlockView(
    block: ScribbleBlock,
    onChange: (ScribbleBlock, Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    val strokeColor = MaterialTheme.colorScheme.onSurface
    val current by rememberUpdatedState(block)
    val livePoints = remember { mutableStateListOf<Offset>() }
    val readOnly = LocalReadOnly.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Gesture,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Scribble",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TableCtrl(Icons.Rounded.Undo, "Undo") {
                if (current.strokes.isNotEmpty()) {
                    onChange(current.copy(strokes = current.strokes.dropLast(1)), true)
                }
            }
            TableCtrl(Icons.Rounded.Delete, "Clear") {
                if (current.strokes.isNotEmpty()) onChange(current.copy(strokes = emptyList()), true)
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !readOnly, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove scribble",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(block.heightDp.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .then(
                    if (readOnly) Modifier
                    else Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            livePoints.clear()
                            livePoints.add(down.position)
                            var active = true
                            while (active) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    active = false
                                } else {
                                    livePoints.add(change.position)
                                    change.consume()
                                }
                            }
                            if (livePoints.isNotEmpty()) {
                                onChange(current.copy(strokes = current.strokes + listOf(livePoints.toList())), true)
                            }
                            livePoints.clear()
                        }
                    },
                ),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                fun drawStroke(points: List<Offset>) {
                    when {
                        points.size == 1 -> drawCircle(strokeColor, radius = 1.6.dp.toPx(), center = points[0])
                        points.size > 1 -> {
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
                block.strokes.forEach { drawStroke(it) }
                if (livePoints.isNotEmpty()) drawStroke(livePoints.toList())
            }
            if (block.strokes.isEmpty() && livePoints.isEmpty()) {
                Text(
                    text = "Draw with your finger or a stylus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .pointerInput(Unit) {
                    var startH = 0
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { startH = current.heightDp; acc = 0f },
                        onDrag = { change, drag ->
                            change.consume()
                            acc += drag.y
                            val delta = with(density) { acc.toDp().value }.toInt()
                            onChange(current.copy(heightDp = (startH + delta).coerceIn(120, 600)), false)
                        },
                        onDragEnd = { onChange(current, true) },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun TableSizeDialog(
    onCreate: (rows: Int, cols: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var rows by remember { mutableStateOf(3) }
    var cols by remember { mutableStateOf(3) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.TableChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Insert table") },
        text = {
            Column {
                TableStepper("Rows", rows, 1, 20) { rows = it }
                Spacer(Modifier.height(12.dp))
                TableStepper("Columns", cols, 1, 8) { cols = it }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(rows, cols) }) { Text("Insert") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TableStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        TableCtrl(Icons.Rounded.Remove, "Decrease") { onChange((value - 1).coerceAtLeast(min)) }
        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TableCtrl(Icons.Rounded.Add, "Increase") { onChange((value + 1).coerceAtMost(max)) }
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

// ---- Inline block model --------------------------------------------------------
//
// A note body is a flat string, but images can live anywhere inside it as a token
// on their own line (see AttachmentMarkup). For editing we split that string into an
// ordered list of text and image blocks, then serialize back to a string on save.

private sealed interface EditorBlock {
    val id: String
}

private data class TextBlock(override val id: String, val value: TextFieldValue) : EditorBlock

private data class ImageBlock(
    override val id: String,
    val fileName: String,
    val widthPercent: Int? = null,
) : EditorBlock

private data class AudioBlock(override val id: String, val fileName: String) : EditorBlock

private data class ChecklistEntry(val id: String, val text: String, val checked: Boolean)

private data class ChecklistBlock(
    override val id: String,
    val items: List<ChecklistEntry>,
) : EditorBlock

private data class TableBlock(
    override val id: String,
    val columnWidths: List<Int>,
    val cells: List<List<String>>,
) : EditorBlock

private data class CalloutBlock(
    override val id: String,
    val emoji: String,
    val text: String,
) : EditorBlock

private data class ScribbleBlock(
    override val id: String,
    val strokes: List<List<Offset>>,
    val heightDp: Int,
) : EditorBlock

private fun newBlockId(): String = UUID.randomUUID().toString()

private val CHECKLIST_LINE = Regex("""^- \[( |x|X)] ?(.*)$""")
private val TABLE_LINE = Regex("""^\[\[table:([A-Za-z0-9+/=]+)]]$""")
private val CALLOUT_LINE = Regex("""^\[\[callout:([A-Za-z0-9+/=]+)]]$""")
private val SCRIBBLE_LINE = Regex("""^\[\[scribble:([A-Za-z0-9+/=]+)]]$""")

private fun parseContentToBlocks(content: String): List<EditorBlock> {
    val result = mutableListOf<EditorBlock>()
    val textLines = mutableListOf<String>()
    val checkItems = mutableListOf<ChecklistEntry>()
    fun flushText() {
        result.add(TextBlock(newBlockId(), TextFieldValue(textLines.joinToString("\n"))))
        textLines.clear()
    }
    fun flushChecklist() {
        if (checkItems.isNotEmpty()) {
            result.add(ChecklistBlock(newBlockId(), checkItems.toList()))
            checkItems.clear()
        }
    }
    content.split("\n").forEach { line ->
        val ref = AttachmentMarkup.parseLine(line)
        val tableMatch = TABLE_LINE.matchEntire(line.trim())
        val calloutMatch = CALLOUT_LINE.matchEntire(line.trim())
        val scribbleMatch = SCRIBBLE_LINE.matchEntire(line.trim())
        val checkMatch = CHECKLIST_LINE.matchEntire(line)
        when {
            ref != null -> {
                flushChecklist(); flushText()
                if (ref.kind == AttachmentKind.AUDIO) result.add(AudioBlock(newBlockId(), ref.fileName))
                else result.add(ImageBlock(newBlockId(), ref.fileName, ref.widthPercent))
            }
            tableMatch != null -> {
                flushChecklist(); flushText()
                val table = decodeTable(newBlockId(), tableMatch.groupValues[1])
                if (table != null) result.add(table) else textLines.add(line)
            }
            calloutMatch != null -> {
                flushChecklist(); flushText()
                val callout = decodeCallout(newBlockId(), calloutMatch.groupValues[1])
                if (callout != null) result.add(callout) else textLines.add(line)
            }
            scribbleMatch != null -> {
                flushChecklist(); flushText()
                val scribble = decodeScribble(newBlockId(), scribbleMatch.groupValues[1])
                if (scribble != null) result.add(scribble) else textLines.add(line)
            }
            checkMatch != null -> {
                // Only break the text run when a checklist run actually starts.
                if (checkItems.isEmpty()) flushText()
                checkItems.add(
                    ChecklistEntry(
                        newBlockId(),
                        checkMatch.groupValues[2],
                        checkMatch.groupValues[1].equals("x", ignoreCase = true),
                    ),
                )
            }
            else -> {
                flushChecklist()
                textLines.add(line)
            }
        }
    }
    flushChecklist()
    // Always keep a trailing text block so there is somewhere to type after a block.
    flushText()
    return result
}

private fun serializeBlocks(blocks: List<EditorBlock>): String =
    blocks.joinToString("\n") { block ->
        when (block) {
            is TextBlock -> block.value.text
            is ImageBlock -> AttachmentMarkup.imageToken(block.fileName, block.widthPercent)
            is AudioBlock -> AttachmentMarkup.audioToken(block.fileName)
            is ChecklistBlock -> block.items.joinToString("\n") { item ->
                "- [${if (item.checked) "x" else " "}] ${item.text}"
            }
            is TableBlock -> encodeTable(block)
            is CalloutBlock -> encodeCallout(block)
            is ScribbleBlock -> encodeScribble(block)
        }
    }

private fun encodeCallout(block: CalloutBlock): String {
    val obj = JSONObject().put("e", block.emoji).put("t", block.text)
    val bytes = obj.toString().toByteArray(Charsets.UTF_8)
    return "[[callout:${Base64.encodeToString(bytes, Base64.NO_WRAP)}]]"
}

private fun decodeCallout(id: String, encoded: String): CalloutBlock? = runCatching {
    val json = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
    val obj = JSONObject(json)
    CalloutBlock(id, obj.optString("e", "💡").ifBlank { "💡" }, obj.optString("t"))
}.getOrNull()

private fun encodeScribble(block: ScribbleBlock): String {
    val obj = JSONObject()
    obj.put("h", block.heightDp)
    val strokesArr = JSONArray()
    block.strokes.forEach { stroke ->
        val pts = JSONArray()
        stroke.forEach { p -> pts.put(JSONArray().put(p.x.toDouble()).put(p.y.toDouble())) }
        strokesArr.put(pts)
    }
    obj.put("s", strokesArr)
    val bytes = obj.toString().toByteArray(Charsets.UTF_8)
    return "[[scribble:${Base64.encodeToString(bytes, Base64.NO_WRAP)}]]"
}

private fun decodeScribble(id: String, encoded: String): ScribbleBlock? = runCatching {
    val json = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
    val obj = JSONObject(json)
    val h = obj.optInt("h", 220)
    val sArr = obj.optJSONArray("s") ?: JSONArray()
    val strokes = (0 until sArr.length()).map { i ->
        val ptsArr = sArr.getJSONArray(i)
        (0 until ptsArr.length()).map { j ->
            val p = ptsArr.getJSONArray(j)
            Offset(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
        }
    }
    ScribbleBlock(id, strokes, h.coerceIn(120, 600))
}.getOrNull()

private fun encodeTable(block: TableBlock): String {
    val obj = JSONObject()
    obj.put("w", JSONArray(block.columnWidths))
    val rows = JSONArray()
    block.cells.forEach { row -> rows.put(JSONArray(row)) }
    obj.put("c", rows)
    val bytes = obj.toString().toByteArray(Charsets.UTF_8)
    return "[[table:${Base64.encodeToString(bytes, Base64.NO_WRAP)}]]"
}

private fun decodeTable(id: String, encoded: String): TableBlock? = runCatching {
    val json = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
    val obj = JSONObject(json)
    val wArr = obj.getJSONArray("w")
    val widths = (0 until wArr.length()).map { wArr.getInt(it) }
    val cArr = obj.getJSONArray("c")
    val cells = (0 until cArr.length()).map { r ->
        val row = cArr.getJSONArray(r)
        (0 until row.length()).map { row.getString(it) }
    }
    if (cells.isEmpty() || widths.isEmpty()) null else TableBlock(id, widths, cells)
}.getOrNull()

private fun emptyTable(rows: Int, cols: Int): TableBlock = TableBlock(
    id = newBlockId(),
    columnWidths = List(cols) { 130 },
    cells = List(rows) { List(cols) { "" } },
)

private fun TableBlock.setCell(r: Int, c: Int, value: String): TableBlock =
    copy(cells = cells.mapIndexed { ri, row ->
        if (ri == r) row.mapIndexed { ci, cell -> if (ci == c) value else cell } else row
    })

private fun TableBlock.setColumnWidth(c: Int, width: Int): TableBlock =
    copy(columnWidths = columnWidths.mapIndexed { i, w -> if (i == c) width else w })

private fun TableBlock.addRow(): TableBlock = copy(cells = cells + listOf(List(columnWidths.size) { "" }))

private fun TableBlock.removeRow(): TableBlock =
    if (cells.size <= 1) this else copy(cells = cells.dropLast(1))

private fun TableBlock.addColumn(): TableBlock =
    copy(columnWidths = columnWidths + 130, cells = cells.map { it + "" })

private fun TableBlock.removeColumn(): TableBlock =
    if (columnWidths.size <= 1) this
    else copy(columnWidths = columnWidths.dropLast(1), cells = cells.map { it.dropLast(1) })
