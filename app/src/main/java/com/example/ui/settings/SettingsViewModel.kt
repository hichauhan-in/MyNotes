package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.settings.AppSettings
import com.example.di.AppContainer
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val repository = AppContainer.settingsRepository!!

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

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
}
