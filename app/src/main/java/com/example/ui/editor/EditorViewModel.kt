package com.example.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.Note
import com.example.domain.model.NoteType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

enum class SaveStatus { Idle, Editing, Saving, Saved }

data class EditorUiState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.TEXT,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val colorArgb: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attachments: List<String> = emptyList(),
    val saveStatus: SaveStatus = SaveStatus.Idle,
) {
    val wordCount: Int
        get() = content.trim().let { if (it.isEmpty()) 0 else it.split(Regex("\\s+")).size }

    val charCount: Int get() = content.length

    /** Reading time in minutes (~200 wpm), at least 1 when there is content. */
    val readingMinutes: Int
        get() = if (wordCount == 0) 0 else max(1, (wordCount / 200.0).roundToInt())

    /** Speaking time in minutes (~130 wpm). */
    val speakingMinutes: Int
        get() = if (wordCount == 0) 0 else max(1, (wordCount / 130.0).roundToInt())

    val hasContent: Boolean get() = title.isNotBlank() || content.isNotBlank() || attachments.isNotEmpty()
}

class EditorViewModel : ViewModel() {
    private val repository = AppContainer.noteRepository!!
    private val settings = AppContainer.settingsRepository!!
    private val appScope = AppContainer.applicationScope

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var persisted = false
    private var autoSaveJob: Job? = null
    private var loaded = false

    /** True only once the user has actually changed something in this session. */
    private var dirty = false

    fun load(id: String?, template: String?) {
        if (loaded) return
        loaded = true
        if (id.isNullOrBlank()) {
            if (template != null && template.startsWith("custom:")) {
                val templateId = template.removePrefix("custom:")
                viewModelScope.launch {
                    val t = settings.customTemplates.first().find { it.id == templateId }
                    _state.value = if (t != null) {
                        EditorUiState(title = t.name, content = t.content)
                    } else {
                        EditorUiState()
                    }
                }
            } else {
                _state.value = seedFor(template)
            }
            return
        }
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            if (note != null) {
                persisted = true
                _state.value = EditorUiState(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    type = note.type,
                    isPinned = note.isPinned,
                    isFavorite = note.isFavorite,
                    colorArgb = note.colorArgb,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    attachments = note.attachments,
                    saveStatus = SaveStatus.Saved,
                )
            }
        }
    }

    fun onTitleChanged(value: String) {
        dirty = true
        _state.value = _state.value.copy(title = value, saveStatus = SaveStatus.Editing)
        scheduleAutoSave()
    }

    fun onContentChanged(value: String) {
        dirty = true
        _state.value = _state.value.copy(content = value, saveStatus = SaveStatus.Editing)
        scheduleAutoSave()
    }

    fun togglePin() {
        dirty = true
        _state.value = _state.value.copy(isPinned = !_state.value.isPinned)
        persistNow()
    }

    fun toggleFavorite() {
        dirty = true
        _state.value = _state.value.copy(isFavorite = !_state.value.isFavorite)
        persistNow()
    }

    fun setColor(colorArgb: Int) {
        dirty = true
        _state.value = _state.value.copy(colorArgb = colorArgb)
        persistNow()
    }

    fun addAttachment(fileName: String) {
        dirty = true
        _state.value = _state.value.copy(attachments = _state.value.attachments + fileName)
        persistNow()
    }

    fun removeAttachment(fileName: String) {
        dirty = true
        _state.value = _state.value.copy(attachments = _state.value.attachments - fileName)
        persistNow()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(700)
            persistNow()
        }
    }

    /** Save immediately (e.g. when leaving the screen). */
    fun flush() {
        autoSaveJob?.cancel()
        persistNow()
    }

    private fun persistNow() {
        // Never save a note the user hasn't actually touched (e.g. opening a template
        // and backing out, or opening an existing note and leaving unchanged).
        if (!dirty) return
        val snapshot = _state.value
        if (!snapshot.hasContent && !persisted) return
        dirty = false
        appScope.launch {
            _state.value = _state.value.copy(saveStatus = SaveStatus.Saving)
            repository.saveNote(
                Note(
                    id = snapshot.id,
                    title = snapshot.title,
                    content = snapshot.content,
                    type = snapshot.type,
                    createdAt = snapshot.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    isPinned = snapshot.isPinned,
                    isFavorite = snapshot.isFavorite,
                    colorArgb = snapshot.colorArgb,
                    attachments = snapshot.attachments,
                )
            )
            persisted = true
            _state.value = _state.value.copy(saveStatus = SaveStatus.Saved)
        }
    }

    /** Move the note to Trash. Returns via [onDone] once complete. */
    fun deleteToTrash(onDone: () -> Unit) {
        autoSaveJob?.cancel()
        appScope.launch {
            if (persisted) repository.setTrashed(_state.value.id, true)
        }
        onDone()
    }

    private fun seedFor(template: String?): EditorUiState = when (template) {
        "checklist" -> EditorUiState(
            title = "Checklist",
            type = NoteType.CHECKLIST,
            content = "[ ] \n[ ] \n[ ] ",
        )

        "meeting" -> EditorUiState(
            title = "Meeting notes",
            content = buildString {
                append("Attendees:\n\n")
                append("Agenda:\n\n")
                append("Discussion:\n\n")
                append("Action items:\n- [ ] \n\n")
                append("Decisions:\n\n")
                append("Next meeting:")
            },
        )

        else -> EditorUiState()
    }
}
