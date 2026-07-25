package com.example.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.CustomTemplate
import com.example.domain.model.Folder
import com.example.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Rounded.GridView),
    RECENT("Recent", Icons.Rounded.Schedule),
    FAVORITES("Favorites", Icons.Rounded.Favorite),
    PINNED("Pinned", Icons.Rounded.PushPin),
    ARCHIVED("Archived", Icons.Rounded.Archive),
    TRASH("Trash", Icons.Rounded.Delete),
}

/** A book (folder) plus how much it contains, for display on the home screen. */
data class BookItem(
    val folder: Folder,
    val noteCount: Int,
    val subBookCount: Int,
)

data class HomeUiState(
    val filter: NoteFilter = NoteFilter.ALL,
    val query: String = "",
    val pinned: List<Note> = emptyList(),
    val notes: List<Note> = emptyList(),
    val books: List<BookItem> = emptyList(),
    val currentFolderId: String? = null,
    /** Path of books from the top level down to the current one (empty at the root). */
    val breadcrumb: List<Folder> = emptyList(),
    val totalNotes: Int = 0,
    val loading: Boolean = true,
) {
    val currentBook: Folder? get() = breadcrumb.lastOrNull()
    val isEmpty: Boolean get() = pinned.isEmpty() && notes.isEmpty() && books.isEmpty()
    val sectionTitle: String
        get() = when (filter) {
            NoteFilter.ALL -> if (currentBook != null) "Notes" else "All notes"
            NoteFilter.RECENT -> "Recently edited"
            NoteFilter.FAVORITES -> "Favorites"
            NoteFilter.PINNED -> "Pinned"
            NoteFilter.ARCHIVED -> "Archived"
            NoteFilter.TRASH -> "Trash"
        }
}

class HomeViewModel : ViewModel() {
    private val repository = AppContainer.noteRepository!!
    private val folders = AppContainer.folderRepository!!
    private val settings = AppContainer.settingsRepository!!

    val trashRetentionDays: StateFlow<Int> = settings.settings
        .map { it.trashRetentionDays }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    fun setTrashRetention(days: Int) = viewModelScope.launch {
        settings.setTrashRetentionDays(days)
    }

    val customTemplates: StateFlow<List<CustomTemplate>> = settings.customTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every book, flat - used by the "move to book" picker. */
    val allFolders: StateFlow<List<Folder>> = folders.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCustomTemplate(name: String, iconKey: String, content: String) = viewModelScope.launch {
        settings.addTemplate(CustomTemplate(name = name, iconKey = iconKey, content = content))
    }

    fun updateCustomTemplate(id: String, name: String, iconKey: String, content: String) =
        viewModelScope.launch {
            settings.updateTemplate(
                CustomTemplate(id = id, name = name, iconKey = iconKey, content = content),
            )
        }

    fun deleteCustomTemplate(id: String) = viewModelScope.launch {
        settings.deleteTemplate(id)
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(NoteFilter.ALL)
    val filter: StateFlow<NoteFilter> = _filter.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.allNotes,
            folders.allFolders,
            _query,
            _filter,
            _currentFolderId,
        ) { notes, folderList, query, filter, currentFolderId ->
            buildState(notes, folderList, query, filter, currentFolderId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    private fun buildState(
        all: List<Note>,
        folderList: List<Folder>,
        query: String,
        filter: NoteFilter,
        currentFolderId: String?,
    ): HomeUiState {
        // Only the "All" view browses books; the other filters are global smart-lists.
        // While searching, "All" also spans every book so a tag search finds them all.
        val visible = when (filter) {
            NoteFilter.ALL ->
                if (query.isBlank()) {
                    all.filter { !it.isTrashed && !it.isArchived && it.folderId == currentFolderId }
                } else {
                    all.filter { !it.isTrashed && !it.isArchived }
                }
            NoteFilter.RECENT -> all.filter { !it.isTrashed && !it.isArchived }
                .sortedByDescending { it.updatedAt }
            NoteFilter.FAVORITES -> all.filter { !it.isTrashed && it.isFavorite }
            NoteFilter.PINNED -> all.filter { !it.isTrashed && it.isPinned }
            NoteFilter.ARCHIVED -> all.filter { !it.isTrashed && it.isArchived }
            NoteFilter.TRASH -> all.filter { it.isTrashed }
        }

        val searched = if (query.isBlank()) visible else visible.filter { note ->
            note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.tags.any { it.contains(query, ignoreCase = true) }
        }

        val showPinnedSection = filter == NoteFilter.ALL && query.isBlank()
        val pinned = if (showPinnedSection) searched.filter { it.isPinned } else emptyList()
        val rest = if (showPinnedSection) searched.filter { !it.isPinned } else searched

        val books = if (filter == NoteFilter.ALL && query.isBlank()) {
            folderList
                .filter { it.parentId == currentFolderId }
                .sortedBy { it.name.lowercase() }
                .map { folder ->
                    BookItem(
                        folder = folder,
                        noteCount = all.count { !it.isTrashed && it.folderId == folder.id },
                        subBookCount = folderList.count { it.parentId == folder.id },
                    )
                }
        } else {
            emptyList()
        }

        val breadcrumb = if (filter == NoteFilter.ALL) buildBreadcrumb(currentFolderId, folderList)
        else emptyList()

        return HomeUiState(
            filter = filter,
            query = query,
            pinned = pinned,
            notes = rest,
            books = books,
            currentFolderId = if (filter == NoteFilter.ALL) currentFolderId else null,
            breadcrumb = breadcrumb,
            totalNotes = all.count { !it.isTrashed && !it.isArchived },
            loading = false,
        )
    }

    private fun buildBreadcrumb(currentId: String?, folderList: List<Folder>): List<Folder> {
        if (currentId == null) return emptyList()
        val byId = folderList.associateBy { it.id }
        val path = ArrayDeque<Folder>()
        var id: String? = currentId
        val seen = mutableSetOf<String>()
        while (id != null && seen.add(id)) {
            val folder = byId[id] ?: break
            path.addFirst(folder)
            id = folder.parentId
        }
        return path.toList()
    }

    fun onQueryChanged(value: String) { _query.value = value }

    fun onFilterChanged(value: NoteFilter) {
        _filter.value = value
        clearSelection()
    }

    /** The book new notes should be created in, given the current context. */
    fun creationFolderId(): String? =
        if (_filter.value == NoteFilter.ALL) _currentFolderId.value else null

    // ---- Book navigation ----
    fun openBook(folderId: String) {
        _filter.value = NoteFilter.ALL
        _currentFolderId.value = folderId
        clearSelection()
    }

    fun exitToRoot() {
        _currentFolderId.value = null
        clearSelection()
    }

    fun goUp() {
        val crumb = uiState.value.breadcrumb
        _currentFolderId.value = if (crumb.size >= 2) crumb[crumb.size - 2].id else null
        clearSelection()
    }

    // ---- Book CRUD ----
    fun createBook(name: String) = viewModelScope.launch {
        val parent = if (_filter.value == NoteFilter.ALL) _currentFolderId.value else null
        folders.createFolder(name, parent)
    }

    fun renameBook(id: String, name: String) = viewModelScope.launch {
        folders.renameFolder(id, name)
    }

    fun deleteBook(id: String) {
        if (_currentFolderId.value == id) {
            val crumb = uiState.value.breadcrumb
            _currentFolderId.value = if (crumb.size >= 2) crumb[crumb.size - 2].id else null
        }
        viewModelScope.launch { folders.deleteFolder(id) }
    }

    fun moveBook(id: String, newParentId: String?) = viewModelScope.launch {
        folders.moveFolder(id, newParentId)
    }

    // ---- Moving notes into books ----
    fun moveNoteToBook(noteId: String, folderId: String?) = viewModelScope.launch {
        folders.moveNoteToFolder(noteId, folderId)
    }

    fun moveSelectedToBook(folderId: String?) {
        val ids = _selectedIds.value.toList()
        _selectedIds.value = emptySet()
        viewModelScope.launch { folders.moveNotesToFolder(ids, folderId) }
    }

    // ---- Multi-select ----
    fun toggleSelection(id: String) {
        val current = _selectedIds.value.toMutableSet()
        if (!current.add(id)) current.remove(id)
        _selectedIds.value = current
    }

    fun selectAll(ids: List<String>) { _selectedIds.value = ids.toSet() }

    fun clearSelection() { _selectedIds.value = emptySet() }

    private fun runOnSelection(action: suspend (String) -> Unit) {
        val ids = _selectedIds.value.toList()
        _selectedIds.value = emptySet()
        viewModelScope.launch { ids.forEach { action(it) } }
    }

    fun bulkPin() = runOnSelection { repository.setPinned(it, true) }
    fun bulkFavorite() = runOnSelection { repository.setFavorite(it, true) }
    fun bulkArchive() = runOnSelection { repository.setArchived(it, true) }
    fun bulkTrash() = runOnSelection { repository.setTrashed(it, true) }
    fun bulkRestore() = runOnSelection { repository.setTrashed(it, false) }
    fun bulkDeleteForever() = runOnSelection { repository.deletePermanently(it) }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.setPinned(note.id, !note.isPinned) }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch { repository.setFavorite(note.id, !note.isFavorite) }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch { repository.setArchived(note.id, !note.isArchived) }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch { repository.setTrashed(note.id, true) }
    }

    fun restoreFromTrash(note: Note) {
        viewModelScope.launch { repository.setTrashed(note.id, false) }
    }

    fun deleteForever(note: Note) {
        viewModelScope.launch { repository.deletePermanently(note.id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }
}
