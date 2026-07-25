package com.example

import android.app.Application
import com.example.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VaultNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        purgeExpiredTrash()
    }

    /** Permanently removes trashed notes that have outlived the retention window. */
    private fun purgeExpiredTrash() {
        val notes = AppContainer.noteRepository ?: return
        val settings = AppContainer.settingsRepository ?: return
        AppContainer.applicationScope.launch {
            val days = runCatching { settings.settings.first().trashRetentionDays }.getOrDefault(30)
            notes.purgeTrashOlderThan(days)
        }
    }
}
