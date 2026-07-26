package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.FolderRepository
import com.example.data.repository.NoteRepository
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Minimal manual dependency container. Initialised once from [com.example.VaultNotesApp].
 */
object AppContainer {
    private var database: AppDatabase? = null

    /** Process-lifetime scope for work that must outlive a screen (e.g. final autosave). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var noteRepository: NoteRepository? = null
        private set

    var folderRepository: FolderRepository? = null
        private set

    var settingsRepository: SettingsRepository? = null
        private set

    fun init(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "vault_notes_db"
            )
                // Preserve notes across additive schema changes where possible.
                .addMigrations(
                    AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4,
                    AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6,
                )
                // Any other unknown schema jump: wipe & rebuild rather than crash.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            noteRepository = NoteRepository(database!!.noteDao(), context.applicationContext)
            folderRepository = FolderRepository(database!!.folderDao(), database!!.noteDao(), context.applicationContext)
        }
        if (settingsRepository == null) {
            settingsRepository = SettingsRepository(context.applicationContext)
        }
    }
}
