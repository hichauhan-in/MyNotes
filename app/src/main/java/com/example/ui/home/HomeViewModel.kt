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
import com.example.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

data class HomeUiState(
    val filter: NoteFilter = NoteFilter.ALL,
    val query: String = "",
    val pinned: List<Note> = emptyList(),
    val notes: List<Note> = emptyList(),
    val totalNotes: Int = 0,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = pinned.isEmpty() && notes.isEmpty()
    val sectionTitle: String
        get() = when (filter) {
            NoteFilter.ALL -> "All notes"
            NoteFilter.RECENT -> "Recently edited"
            NoteFilter.FAVORITES -> "Favorites"
            NoteFilter.PINNED -> "Pinned"
            NoteFilter.ARCHIVED -> "Archived"
            NoteFilter.TRASH -> "Trash"
        }
}

class HomeViewModel : ViewModel() {
    private val repository = AppContainer.noteRepository!!

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(NoteFilter.ALL)
    val filter: StateFlow<NoteFilter> = _filter.asStateFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(repository.allNotes, _query, _filter) { notes, query, filter ->
            buildState(notes, query, filter)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    private fun buildState(all: List<Note>, query: String, filter: NoteFilter): HomeUiState {
        val visible = when (filter) {
            NoteFilter.ALL -> all.filter { !it.isTrashed && !it.isArchived }
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

        return HomeUiState(
            filter = filter,
            query = query,
            pinned = pinned,
            notes = rest,
            totalNotes = all.count { !it.isTrashed && !it.isArchived },
            loading = false,
        )
    }

    fun onQueryChanged(value: String) { _query.value = value }
    fun onFilterChanged(value: NoteFilter) { _filter.value = value }

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
