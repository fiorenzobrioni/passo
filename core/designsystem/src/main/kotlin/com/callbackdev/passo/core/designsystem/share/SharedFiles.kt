package com.callbackdev.passo.core.designsystem.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * What the reader shares or saves, by their own hand: a page of the year as an image, an
 * outing's route as an image or a GPX file. Written to the cache and handed to the app the
 * reader picks through the app's one file provider, for that one share; or written where the
 * reader chooses, through the Storage Access Framework. No permission either way, and nothing
 * sent by Passo: it has no network to send with.
 */
object SharedFiles {
    /**
     * Writes a file named [name] to the cache's share folder with [write], which says whether
     * it wrote it; its content Uri, or null if it could not be written. One file at a time: the
     * last share's is no longer needed.
     */
    suspend fun cache(context: Context, name: String, write: (OutputStream) -> Boolean): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
                folder.listFiles()?.forEach { it.delete() }
                val file = File(folder, name)
                if (file.outputStream().use(write)) {
                    FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
                } else {
                    file.delete()
                    null
                }
            } catch (e: IOException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    /** The system's share sheet, with the file readable by the app the reader picks. */
    fun share(context: Context, uri: Uri, mimeType: String, title: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        runCatching { context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /**
     * Writes with [write], which says whether it wrote it, where the reader chose (the Storage
     * Access Framework); false if it could not: a stream that opened is not a file written.
     */
    suspend fun save(context: Context, uri: Uri, write: (OutputStream) -> Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use(write) == true
            } catch (e: IOException) {
                false
            } catch (e: SecurityException) {
                false
            }
        }

    private const val FOLDER = "share"
    private const val AUTHORITY_SUFFIX = ".share"
}

/** Writes [text] as UTF-8, for [SharedFiles]: a document, unlike an image, cannot fail to encode. */
fun OutputStream.writeDocument(text: String): Boolean {
    write(text.toByteArray(Charsets.UTF_8))
    return true
}
