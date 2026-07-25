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
    }

    val customTemplates: Flow<List<CustomTemplate>> = dataStore.data.map { prefs ->
        parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
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

    suspend fun addTemplate(template: CustomTemplate) = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(current + template)
    }

    suspend fun deleteTemplate(id: String) = dataStore.edit { prefs ->
        val current = parseTemplates(prefs[Keys.TEMPLATES] ?: "[]")
        prefs[Keys.TEMPLATES] = serializeTemplates(current.filterNot { it.id == id })
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
            )
        }
    }.getOrDefault(emptyList())

    private fun serializeTemplates(items: List<CustomTemplate>): String {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("icon", it.iconKey)
                    .put("content", it.content),
            )
        }
        return arr.toString()
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
