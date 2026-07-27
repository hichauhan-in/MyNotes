package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.settings.AppSettings
import com.example.data.sync.DriveRest
import com.example.di.AppContainer
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Transient UI state for the Google Drive connect flow (not persisted). */
data class SyncScreenState(
    val connecting: Boolean = false,
    val error: String? = null,
)

class SettingsViewModel : ViewModel() {
    private val repository = AppContainer.settingsRepository!!

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val _syncState = MutableStateFlow(SyncScreenState())
    val syncState: StateFlow<SyncScreenState> = _syncState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(value) }
    }

    fun setAccentIndex(index: Int) {
        viewModelScope.launch { repository.setAccentIndex(index) }
    }

    fun setCloudSyncEnabled(value: Boolean) {
        viewModelScope.launch { repository.setCloudSyncEnabled(value) }
    }

    fun setAppLockEnabled(value: Boolean) {
        viewModelScope.launch { repository.setAppLockEnabled(value) }
    }

    fun setHideFromRecents(value: Boolean) {
        viewModelScope.launch { repository.setHideFromRecents(value) }
    }

    fun setDefaultExportFolder(uri: String?) {
        viewModelScope.launch { repository.setDefaultExportFolder(uri) }
    }

    // ---- Google Drive sync (Slice 1: connect + verify) ----

    /** Called while the account picker / consent screen is showing. */
    fun onDriveConnecting() {
        _syncState.value = SyncScreenState(connecting = true)
    }

    /**
     * Called with a valid Drive access token once authorization succeeds. Confirms the token works
     * by reading the account email from Drive, then remembers the connected account.
     */
    fun onDriveAuthorized(accessToken: String) {
        _syncState.value = SyncScreenState(connecting = true)
        viewModelScope.launch {
            val email = withContext(Dispatchers.IO) { DriveRest.fetchAccountEmail(accessToken) }
            if (email != null) {
                repository.setDriveAccountEmail(email)
                repository.setCloudSyncEnabled(true)
                _syncState.value = SyncScreenState()
            } else {
                _syncState.value = SyncScreenState(error = "Connected, but couldn't reach Google Drive. Please try again.")
            }
        }
    }

    fun onDriveAuthFailed(message: String?) {
        _syncState.value = SyncScreenState(error = message ?: "Google sign-in was cancelled.")
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            repository.setDriveAccountEmail(null)
            _syncState.value = SyncScreenState()
        }
    }

    fun clearSyncError() {
        _syncState.value = _syncState.value.copy(error = null)
    }
}
