package com.callbackdev.passo.core.designsystem.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.io.OutputStream

/**
 * Something of the reader's as an image to share: 1080 by 1920, what the app drew, as the reader
 * sees it in their theme, on its ground, with Passo's name small at the foot. Nothing but what
 * was drawn: no name, no date of the sharing. A page of the year (Phase 12), an outing's route
 * (Phase 16).
 */
object ShareImage {
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val MARGIN = 72
    private const val FOOT = 140
    private const val SIGNATURE_SIZE = 36f
    private const val QUALITY = 100

    const val MIME_TYPE = "image/png"

    /** [drawn] fitted into the image, on [ground], with [signature] at the foot in [ink]. */
    fun compose(drawn: Bitmap, ground: Int, ink: Int, signature: String): Bitmap {
        val image = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawColor(ground)
        val room = Rect(MARGIN, MARGIN, WIDTH - MARGIN, HEIGHT - FOOT)
        val scale = minOf(room.width().toFloat() / drawn.width, room.height().toFloat() / drawn.height)
        val width = (drawn.width * scale).toInt()
        val height = (drawn.height * scale).toInt()
        val left = room.left + (room.width() - width) / 2
        val top = room.top + (room.height() - height) / 2
        val software = if (drawn.config == Bitmap.Config.HARDWARE) drawn.copy(Bitmap.Config.ARGB_8888, false) else drawn
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

    /** Writes [image] to [out] as a PNG. */
    fun write(image: Bitmap, out: OutputStream) {
        image.compress(Bitmap.CompressFormat.PNG, QUALITY, out)
    }
}
