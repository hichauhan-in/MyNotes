package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Note
import com.example.ui.components.EmptyState
import com.example.ui.components.NeuCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.NeuSurface
import com.example.ui.components.PillChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.TagPill
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.brandGradientHorizontal
import com.example.ui.theme.neumorphicRaised
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNoteClick: (String) -> Unit,
    onCreateNote: (template: String?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    val selectionMode = selectedIds.isNotEmpty()
    val retentionDays by viewModel.trashRetentionDays.collectAsStateWithLifecycle()
    var fabExpanded by remember { mutableStateOf(false) }
    var actionNote by remember { mutableStateOf<Note?>(null) }
    var showCoffeeSheet by remember { mutableStateOf(false) }
    var showRetentionSheet by remember { mutableStateOf(false) }

    val insets = WindowInsets.systemBars.asPaddingValues()
    val visibleIds = remember(state.pinned, state.notes) {
        state.pinned.map { it.id } + state.notes.map { it.id }
    }

    // In selection mode, the back gesture exits selection instead of closing the app.
    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(168.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = insets.calculateTopPadding() + 10.dp,
                bottom = insets.calculateBottomPadding() + 128.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalItemSpacing = 14.dp,
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                if (selectionMode) {
                    SelectionTopBar(
                        count = selectedIds.size,
                        allSelected = visibleIds.isNotEmpty() && selectedIds.size >= visibleIds.size,
                        onClose = viewModel::clearSelection,
                        onToggleSelectAll = {
                            if (selectedIds.size >= visibleIds.size) viewModel.clearSelection()
                            else viewModel.selectAll(visibleIds)
                        },
                    )
                } else {
                    HomeHeader(
                        noteCount = state.totalNotes,
                        onCoffee = { showCoffeeSheet = true },
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    SearchField(
                        value = query,
                        onValueChange = viewModel::onQueryChanged,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    FilterChipsRow(
                        selected = selectedFilter,
                        onSelect = viewModel::onFilterChanged,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            if (selectedFilter == NoteFilter.TRASH && !state.isEmpty) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        TrashBanner(
                            retentionDays = retentionDays,
                            onChangeRetention = { showRetentionSheet = true },
                            onEmptyTrash = viewModel::emptyTrash,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            if (state.pinned.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        SectionHeader(title = "Pinned")
                        Spacer(Modifier.height(12.dp))
                    }
                }
                items(state.pinned, key = { "pin_${it.id}" }) { note ->
                    NoteCard(
                        note = note,
                        selected = note.id in selectedIds,
                        selectionMode = selectionMode,
                        onOpen = { onNoteClick(note.id) },
                        onToggleSelect = { viewModel.toggleSelection(note.id) },
                        onMore = { actionNote = note },
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(Modifier.height(20.dp))
                }
            }

            if (state.notes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        SectionHeader(title = state.sectionTitle)
                        Spacer(Modifier.height(12.dp))
                    }
                }
                items(state.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        selected = note.id in selectedIds,
                        selectionMode = selectionMode,
                        onOpen = { onNoteClick(note.id) },
                        onToggleSelect = { viewModel.toggleSelection(note.id) },
                        onMore = { actionNote = note },
                    )
                }
            }

            if (state.isEmpty && !state.loading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        Spacer(Modifier.height(40.dp))
                        EmptyState(
                            icon = emptyIconFor(selectedFilter),
                            title = emptyTitleFor(selectedFilter, query),
                            subtitle = emptySubtitleFor(selectedFilter),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Dim scrim behind the expanded FAB menu.
        AnimatedVisibility(
            visible = fabExpanded && !selectionMode,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { fabExpanded = false },
            )
        }

        if (!selectionMode) {
            ExpandableFab(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                onAction = { template ->
                    fabExpanded = false
                    onCreateNote(template)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = insets.calculateBottomPadding() + 24.dp),
            )
        }

        // Contextual bulk-action bar while multiple notes are selected.
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SelectionActionBar(
                filter = selectedFilter,
                onPin = viewModel::bulkPin,
                onFavorite = viewModel::bulkFavorite,
                onArchive = viewModel::bulkArchive,
                onTrash = viewModel::bulkTrash,
                onRestore = viewModel::bulkRestore,
                onDeleteForever = viewModel::bulkDeleteForever,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = insets.calculateBottomPadding() + 20.dp),
            )
        }
    }

    val current = actionNote
    if (current != null) {
        NoteActionsSheet(
            note = current,
            filter = selectedFilter,
            onDismiss = { actionNote = null },
            viewModel = viewModel,
        )
    }

    if (showCoffeeSheet) {
        BuyCoffeeSheet(onDismiss = { showCoffeeSheet = false })
    }

    if (showRetentionSheet) {
        RetentionPickerSheet(
            current = retentionDays,
            onSelect = {
                viewModel.setTrashRetention(it)
                showRetentionSheet = false
            },
            onDismiss = { showRetentionSheet = false },
        )
    }
}

@Composable
private fun HomeHeader(
    noteCount: Int,
    onCoffee: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "MyNotes+",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (noteCount == 0) "Encrypted & private" else "$noteCount encrypted notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        NeuIconButton(
            icon = Icons.Rounded.Coffee,
            contentDescription = "Buy me a coffee",
            onClick = onCoffee,
        )
        Spacer(Modifier.width(10.dp))
        NeuIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeuIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Cancel selection",
            onClick = onClose,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        NeuIconButton(
            icon = Icons.Rounded.SelectAll,
            contentDescription = if (allSelected) "Deselect all" else "Select all",
            onClick = onToggleSelectAll,
            tint = if (allSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectionActionBar(
    filter: NoteFilter,
    onPin: () -> Unit,
    onFavorite: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeuSurface(
        cornerRadius = 26.dp,
        elevation = 10.dp,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (filter == NoteFilter.TRASH) {
                SelectionActionItem(Icons.Rounded.Restore, "Restore", onRestore)
                SelectionActionItem(Icons.Rounded.DeleteForever, "Delete", onDeleteForever, destructive = true)
            } else {
                SelectionActionItem(Icons.Rounded.PushPin, "Pin", onPin)
                SelectionActionItem(Icons.Rounded.FavoriteBorder, "Favorite", onFavorite)
                SelectionActionItem(Icons.Rounded.Archive, "Archive", onArchive)
                SelectionActionItem(Icons.Rounded.Delete, "Trash", onTrash, destructive = true)
            }
        }
    }
}

@Composable
private fun SelectionActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun SelectionBadge(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(10.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
            .border(
                width = 1.5.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val neu = LocalNeuColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(20.dp, neu, elevation = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Search your notes…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(visible = value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: NoteFilter,
    onSelect: (NoteFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NoteFilter.entries.forEach { filter ->
            PillChip(
                label = filter.label,
                selected = filter == selected,
                onClick = { onSelect(filter) },
                leadingIcon = filter.icon,
            )
        }
    }
}

@Composable
private fun TrashBanner(
    retentionDays: Int,
    onChangeRetention: () -> Unit,
    onEmptyTrash: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Auto-delete after",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (retentionDays <= 0) "Never" else "$retentionDays days",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onChangeRetention)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Deleted notes are removed permanently.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Empty now",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onEmptyTrash)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onToggleSelect: () -> Unit,
    onMore: () -> Unit,
) {
    val accent = if (note.colorArgb != 0) Color(note.colorArgb) else null
    Box(modifier = Modifier.fillMaxWidth()) {
        NeuCard(
            onClick = { if (selectionMode) onToggleSelect() else onOpen() },
            onLongClick = onToggleSelect,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (accent != null) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(if (note.title.isNotBlank()) 108.dp else 88.dp)
                            .background(accent),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                ) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (note.preview.isNotBlank()) {
                        Text(
                            text = note.preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (note.title.isNotBlank()) 5 else 7,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (note.tags.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            note.tags.take(2).forEach { tag -> TagPill(text = "#$tag") }
                        }
                    }

                    if (note.isChecklist && note.checklistTotal > 0) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${note.checklistDone}/${note.checklistTotal} done",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = relativeTime(note.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                        )
                        if (note.isFavorite) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        if (!selectionMode) {
                            Spacer(Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onMore),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Note options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp)),
            )
        }
        if (selectionMode) {
            SelectionBadge(selected = selected, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun ExpandableFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAction: (template: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeuColors.current
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fabRotation")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.8f),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                FabAction("Meeting", Icons.Rounded.Groups) { onAction("meeting") }
                Spacer(Modifier.height(12.dp))
                FabAction("Checklist", Icons.Rounded.Checklist) { onAction("checklist") }
                Spacer(Modifier.height(12.dp))
                FabAction("New note", Icons.Rounded.EditNote) { onAction(null) }
                Spacer(Modifier.height(16.dp))
            }
        }

        Box(
            modifier = Modifier
                .size(62.dp)
                .neumorphicRaised(31.dp, neu, elevation = 10.dp)
                .clip(CircleShape)
                .background(brandGradientHorizontal())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Create note",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation),
            )
        }
    }
}

@Composable
private fun FabAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val neu = LocalNeuColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .neumorphicRaised(24.dp, neu, elevation = 7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteActionsSheet(
    note: Note,
    filter: NoteFilter,
    onDismiss: () -> Unit,
    viewModel: HomeViewModel,
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
                text = note.title.ifBlank { "Untitled note" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))

            if (filter == NoteFilter.TRASH) {
                SheetAction(Icons.Rounded.Restore, "Restore") {
                    viewModel.restoreFromTrash(note); onDismiss()
                }
                SheetAction(Icons.Rounded.DeleteForever, "Delete forever", destructive = true) {
                    viewModel.deleteForever(note); onDismiss()
                }
            } else {
                SheetAction(
                    Icons.Rounded.PushPin,
                    if (note.isPinned) "Unpin" else "Pin to top",
                ) { viewModel.togglePin(note); onDismiss() }
                SheetAction(
                    if (note.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (note.isFavorite) "Remove favorite" else "Add to favorites",
                ) { viewModel.toggleFavorite(note); onDismiss() }
                SheetAction(
                    if (note.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                    if (note.isArchived) "Unarchive" else "Archive",
                ) { viewModel.toggleArchive(note); onDismiss() }
                SheetAction(Icons.Rounded.Delete, "Move to Trash", destructive = true) {
                    viewModel.moveToTrash(note); onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuyCoffeeSheet(onDismiss: () -> Unit) {
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
                text = "Buy me a coffee ☕",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "If MyNotes+ makes your day a little calmer, you can support its development. These options are on their way.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            CoffeeOption(Icons.Rounded.CurrencyRupee, "UPI", "Pay via any UPI app")
            Spacer(Modifier.height(12.dp))
            CoffeeOption(Icons.Rounded.LocalCafe, "Ko-fi", "Support on ko-fi.com")
            Spacer(Modifier.height(12.dp))
            CoffeeOption(Icons.Rounded.Shop, "Google Play", "Tip through the Play Store")
        }
    }
}

@Composable
private fun CoffeeOption(icon: ImageVector, name: String, subtitle: String) {
    val neu = LocalNeuColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(18.dp, neu, elevation = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.blur(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(brandGradientHorizontal()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "Coming soon",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetentionPickerSheet(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(7, 14, 30, 60, 90, 0)
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
                text = "Keep deleted notes for",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Notes in Trash are permanently deleted after this period.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            options.forEach { days ->
                val selected = days == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(days) }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (days <= 0) "Never (keep forever)" else "$days days",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---- helpers -------------------------------------------------------------------

private fun relativeTime(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(millis)
    }
}

private fun emptyIconFor(filter: NoteFilter): ImageVector = when (filter) {
    NoteFilter.FAVORITES -> Icons.Rounded.FavoriteBorder
    NoteFilter.PINNED -> Icons.Rounded.PushPin
    NoteFilter.ARCHIVED -> Icons.Rounded.Archive
    NoteFilter.TRASH -> Icons.Rounded.Delete
    else -> Icons.Rounded.EditNote
}

private fun emptyTitleFor(filter: NoteFilter, query: String): String = when {
    query.isNotBlank() -> "No matches"
    filter == NoteFilter.FAVORITES -> "No favorites yet"
    filter == NoteFilter.PINNED -> "Nothing pinned"
    filter == NoteFilter.ARCHIVED -> "Archive is empty"
    filter == NoteFilter.TRASH -> "Trash is empty"
    else -> "Your canvas is clear"
}

private fun emptySubtitleFor(filter: NoteFilter): String = when (filter) {
    NoteFilter.FAVORITES -> "Star notes to find them fast."
    NoteFilter.PINNED -> "Pin important notes to keep them on top."
    NoteFilter.ARCHIVED -> "Archived notes are tucked away here."
    NoteFilter.TRASH -> "Deleted notes will appear here."
    else -> "Tap + to write your first encrypted note."
}
