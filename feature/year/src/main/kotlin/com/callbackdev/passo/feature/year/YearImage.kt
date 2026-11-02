package com.callbackdev.passo.feature.year

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * A page of the year as an image (PLANNING.md §11 Phase 12): 1080 by 1920, the page as the reader
 * sees it in their theme, on its ground, with Passo's name small at the foot. Nothing but what
 * the page shows: no name, no place, no date of the sharing. Written to the cache and handed to
 * the app the reader picks, for that share; or saved where the reader chooses.
 */
internal object YearImage {
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val MARGIN = 72
    private const val FOOT = 140
    private const val SIGNATURE_SIZE = 36f
    private const val QUALITY = 100

    /** [page] fitted into the image, on [ground], with [signature] at the foot in [ink]. */
    fun compose(page: Bitmap, ground: Int, ink: Int, signature: String): Bitmap {
        val image = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawColor(ground)
        val room = Rect(MARGIN, MARGIN, WIDTH - MARGIN, HEIGHT - FOOT)
        val scale = minOf(room.width().toFloat() / page.width, room.height().toFloat() / page.height)
        val width = (page.width * scale).toInt()
        val height = (page.height * scale).toInt()
        val left = room.left + (room.width() - width) / 2
        val top = room.top + (room.height() - height) / 2
        val software = if (page.config == Bitmap.Config.HARDWARE) page.copy(Bitmap.Config.ARGB_8888, false) else page
        canvas.drawBitmap(software, null, Rect(left, top, left + width, top + height), Paint(Paint.FILTER_BITMAP_FLAG))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = SIGNATURE_SIZE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(signature, WIDTH / 2f, HEIGHT - FOOT / 2f + SIGNATURE_SIZE / 3, paint)
        return image
    }

    fun fileName(year: Int, page: Int): String = "passo-$year-${page + 1}.png"

    /** Writes [image] to the cache's year folder; its content Uri, or null if it could not. */
    suspend fun cache(context: Context, image: Bitmap, name: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val folder = File(context.cacheDir, "year").apply { mkdirs() }
            // One image at a time: the last share's is no longer needed.
            folder.listFiles()?.forEach { it.delete() }
            val file = File(folder, name)
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, QUALITY, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.year", file)
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** The system's share sheet, with the image readable by the app the reader picks. */
    fun share(context: Context, uri: Uri, title: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        runCatching { context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Writes [image] where the reader chose (the Storage Access Framework); false if it could not. */
    suspend fun save(context: Context, image: Bitmap, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use {
                image.compress(Bitmap.CompressFormat.PNG, QUALITY, it)
            } ==
                true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
