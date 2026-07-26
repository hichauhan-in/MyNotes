package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.CustomTemplate
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "mynotes_settings")

/** Immutable snapshot of every user preference. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val accentIndex: Int = 0,
    val cloudSyncEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val hideFromRecents: Boolean = false,
    /** How many days deleted notes stay in Trash before auto-deletion. 0 == keep forever. */
    val trashRetentionDays: Int = 30,
    val onboardingComplete: Boolean = false,
    /** Persisted SAF tree Uri (as string) of the default export folder, or null if unset. */
    val defaultExportFolder: String? = null,
)

/**
 * Lightweight, non-sensitive preference store backed by Jetpack DataStore.
 * (No note content or keys are ever stored here.)
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val ACCENT = intPreferencesKey("accent_index")
        val CLOUD = booleanPreferencesKey("cloud_sync_enabled")
        val LOCK = booleanPreferencesKey("app_lock_enabled")
        val HIDE_RECENTS = booleanPreferencesKey("hide_from_recents")
        val TRASH_RETENTION = intPreferencesKey("trash_retention_days")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
        val TEMPLATES = stringPreferencesKey("custom_templates")
        val EXPORT_FOLDER = stringPreferencesKey("default_export_folder")
    }

    val customTemplates: Flow<List<CustomTemplate>> = dataStore.data.map { prefs ->
        parseTemplates(prefs[Keys.TEMPLATES] ?: "[]").filter { it.trashedAt == null }
    }

    /** Templates that have been deleted and are recoverable from Trash, newest first. */
    val trashedTemplates: Flow<List<CustomTemplate>> = dataStore.data.map { prefs ->
        parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
            .filter { it.trashedAt != null }
            .sortedByDescending { it.trashedAt }
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[Keys.DYNAMIC] ?: false,
            accentIndex = prefs[Keys.ACCENT] ?: 0,
            cloudSyncEnabled = prefs[Keys.CLOUD] ?: false,
            appLockEnabled = prefs[Keys.LOCK] ?: false,
            hideFromRecents = prefs[Keys.HIDE_RECENTS] ?: false,
            trashRetentionDays = prefs[Keys.TRASH_RETENTION] ?: 30,
            onboardingComplete = prefs[Keys.ONBOARDING_DONE] ?: false,
            defaultExportFolder = prefs[Keys.EXPORT_FOLDER],
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        edit { it[Keys.THEME] = mode.name }

    suspend fun setDynamicColor(value: Boolean) =
        edit { it[Keys.DYNAMIC] = value }

    suspend fun setAccentIndex(index: Int) =
        edit { it[Keys.ACCENT] = index }

    suspend fun setCloudSyncEnabled(value: Boolean) =
        edit { it[Keys.CLOUD] = value }

    suspend fun setAppLockEnabled(value: Boolean) =
        edit { it[Keys.LOCK] = value }

    suspend fun setHideFromRecents(value: Boolean) =
        edit { it[Keys.HIDE_RECENTS] = value }

    suspend fun setTrashRetentionDays(days: Int) =
        edit { it[Keys.TRASH_RETENTION] = days }

    suspend fun setOnboardingComplete(value: Boolean) =
        edit { it[Keys.ONBOARDING_DONE] = value }

    suspend fun setDefaultExportFolder(uri: String?) = edit {
        if (uri == null) it.remove(Keys.EXPORT_FOLDER) else it[Keys.EXPORT_FOLDER] = uri
    }

    suspend fun addTemplate(template: CustomTemplate) = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(current + template)
    }

    suspend fun updateTemplate(template: CustomTemplate) = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(
            current.map { if (it.id == template.id) template else it },
        )
    }

    suspend fun deleteTemplate(id: String) = dataStore.edit { prefs ->
        // Soft delete: move the template into Trash so it can be recovered for the retention window.
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(
            current.map {
                if (it.id == id && it.trashedAt == null) it.copy(trashedAt = System.currentTimeMillis()) else it
            },
        )
    }

    suspend fun restoreTemplate(id: String) = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(
            current.map { if (it.id == id) it.copy(trashedAt = null) else it },
        )
    }

    suspend fun deleteTemplatePermanently(id: String) = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(current.filterNot { it.id == id })
    }

    /** Permanently removes trashed templates deleted before [cutoff] (epoch millis). */
    suspend fun purgeTrashedTemplatesBefore(cutoff: Long) = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(
            current.filterNot { it.trashedAt != null && it.trashedAt < cutoff },
        )
    }

    /** Permanently removes every trashed template (used by "Empty Trash"). */
    suspend fun emptyTrashedTemplates() = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(current.filter { it.trashedAt == null })
    }

    private fun parseTemplates(json: String): List<CustomTemplate> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CustomTemplate(
                id = o.optString("id"),
                name = o.optString("name"),
                iconKey = o.optString("icon", "note"),
                content = o.optString("content"),
                trashedAt = if (o.has("trashedAt") && !o.isNull("trashedAt")) o.optLong("trashedAt") else null,
            )
        }
    }.getOrDefault(emptyList())

    private fun serializeTemplates(items: List<CustomTemplate>): String {
        val arr = JSONArray()
        items.forEach {
            val o = JSONObject()
                .put("id", it.id)
                .put("name", it.name)
                .put("icon", it.iconKey)
                .put("content", it.content)
            if (it.trashedAt != null) o.put("trashedAt", it.trashedAt)
            arr.put(o)
        }
        return arr.toString()
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
