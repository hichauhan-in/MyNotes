package com.example.data.sync

import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope

/**
 * Builds the Google "Authorization" request for cloud sync. We ask only for the two narrow,
 * non-sensitive scopes:
 *  - drive.file    : files this app creates/opens (the visible "MyNotes" sync folder)
 *  - drive.appdata : the app's private, hidden folder (holds the wrapped encryption keys)
 * Requesting only these avoids Google's restricted-scope verification/security assessment.
 */
object DriveAuth {
    const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

    fun request(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE_DRIVE_FILE), Scope(SCOPE_DRIVE_APPDATA)))
            .build()
}
