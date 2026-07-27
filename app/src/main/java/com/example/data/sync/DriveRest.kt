package com.example.data.sync

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin Google Drive v3 REST client used by cloud sync. It only ever sends already-encrypted bytes,
 * and talks to Drive with a short-lived OAuth access token obtained via the Authorization API.
 *
 * Slice 1 only needs [fetchAccountEmail] to prove the whole OAuth chain works end-to-end; the
 * folder/file/appData operations are added in later slices.
 */
object DriveRest {

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Returns the email of the account the [accessToken] belongs to (via Drive's `about` endpoint),
     * or null on any failure. A successful non-null result confirms the token is valid and the app
     * is authorised for Drive.
     */
    fun fetchAccountEmail(accessToken: String): String? = runCatching {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/about?fields=user(emailAddress,displayName)")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            JSONObject(body).optJSONObject("user")?.optString("emailAddress")?.ifBlank { null }
        }
    }.getOrNull()
}
