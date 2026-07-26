package com.example.data.attachments

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Stores note image attachments in the app-private files directory. That directory is
 * sandboxed to this app and encrypted at rest by Android's file-based encryption, so the
 * bytes never leave the device. Only the file *names* are persisted alongside the note.
 */
object AttachmentStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "attachments").apply { mkdirs() }

    // Only the last path segment is ever used, so a maliciously-crafted note token like
    // `attachment://../../databases/...` can never resolve outside the attachments directory.
    fun fileFor(context: Context, name: String): File = File(dir(context), File(name).name)

    /** A fresh, empty destination file for a camera capture. */
    fun newImageFile(context: Context): File =
        File(dir(context), "img_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")

    /** A fresh, empty destination file for a recorded voice note. */
    fun newAudioFile(context: Context): File =
        File(dir(context), "aud_${System.currentTimeMillis()}_${UUID.randomUUID()}.m4a")

    /** A content Uri (via FileProvider) that the camera app can write the capture to. */
    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies a picked content Uri into private storage; returns the stored file name or null. */
    fun importFromUri(context: Context, uri: Uri): String? = runCatching {
        val file = newImageFile(context)
        val copied = context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
        if (copied) file.name else null
    }.getOrNull()

    /** Writes a bitmap (e.g. a cropped image) into a fresh private JPEG; returns its name or null. */
    fun saveBitmap(context: Context, bitmap: android.graphics.Bitmap): String? = runCatching {
        val file = newImageFile(context)
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
        }
        file.name
    }.getOrNull()

    fun delete(context: Context, name: String) {
        runCatching { fileFor(context, name).delete() }
    }
}
