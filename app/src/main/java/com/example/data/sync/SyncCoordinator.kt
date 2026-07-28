package com.example.data.sync

import android.content.Context
import com.example.data.settings.SettingsRepository
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Fires an automatic background sync when the app comes to the foreground, so notes edited on one
 * device show up on another without the user having to open Settings and tap "Sync now".
 *
 * It only runs when Drive is already connected and this device has unlocked the key, and it
 * acquires a token *silently* (the Authorization API returns one with no UI when access was already
 * granted). Failures are swallowed - the manual "Sync now" path surfaces errors.
 */
object SyncCoordinator {

    private val inFlight = AtomicBoolean(false)

    /** Skip an automatic foreground sync if we already synced this recently (avoids spamming Drive). */
    private const val MIN_AUTO_SYNC_INTERVAL_MS = 15_000L

    fun autoSync(
        context: Context,
        settings: SettingsRepository,
        manager: CloudSyncManager,
        scope: CoroutineScope,
    ) {
        if (!inFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                val snapshot = settings.snapshot()
                if (snapshot.driveAccountEmail == null || !snapshot.recoveryConfigured) return@launch
                if (System.currentTimeMillis() - snapshot.lastSyncedAt < MIN_AUTO_SYNC_INTERVAL_MS) return@launch
                if (!manager.hasLocalKey()) return@launch
                val token = silentToken(context.applicationContext) ?: return@launch
                manager.syncNow(token)
            } finally {
                inFlight.set(false)
            }
        }
    }

    /** User-initiated sync (the Home "+" tap). No throttle; skipped only if a sync is already running. */
    fun manualSync(
        context: Context,
        settings: SettingsRepository,
        manager: CloudSyncManager,
        scope: CoroutineScope,
    ) {
        if (SyncStatus.syncing.value) return
        scope.launch {
            val snapshot = settings.snapshot()
            if (snapshot.driveAccountEmail == null || !snapshot.recoveryConfigured) return@launch
            if (!manager.hasLocalKey()) return@launch
            val token = silentToken(context.applicationContext) ?: return@launch
            manager.syncNow(token)
        }
    }

    /** Gets a Drive access token without any UI, or null if consent would be required / it fails. */
    private suspend fun silentToken(context: Context): String? = suspendCancellableCoroutine { cont ->
        Identity.getAuthorizationClient(context)
            .authorize(DriveAuth.request())
            .addOnSuccessListener { result ->
                cont.resume(if (!result.hasResolution()) result.accessToken else null)
            }
            .addOnFailureListener { cont.resume(null) }
    }
}
