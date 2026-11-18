package com.callbackdev.passo.feature.year

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.callbackdev.passo.core.designsystem.share.ShareImage
import com.callbackdev.passo.core.designsystem.share.SharedFiles

/**
 * A page of the year as an image (PLANNING.md §11 Phase 12): 1080 by 1920, the page as the reader
 * sees it in their theme, on its ground, with Passo's name small at the foot. Nothing but what
 * the page shows: no name, no place, no date of the sharing. Written to the cache and handed to
 * the app the reader picks, for that share; or saved where the reader chooses. The image and
 * the files are the app's shared ones ([ShareImage], [SharedFiles]), which an outing's route
 * goes through too.
 */
internal object YearImage {
    /** [page] fitted into the image, on [ground], with [signature] at the foot in [ink]. */
    fun compose(page: Bitmap, ground: Int, ink: Int, signature: String): Bitmap =
        ShareImage.compose(page, ground, ink, signature)

    fun fileName(year: Int, page: Int): String = "passo-$year-${page + 1}.png"

    /** Writes [image] to the cache's share folder; its content Uri, or null if it could not. */
    suspend fun cache(context: Context, image: Bitmap, name: String): Uri? =
        SharedFiles.cache(context, name) { ShareImage.write(image, it) }

    /** The system's share sheet, with the image readable by the app the reader picks. */
    fun share(context: Context, uri: Uri, title: String) {
        SharedFiles.share(context, uri, ShareImage.MIME_TYPE, title)
    }

    /** Writes [image] where the reader chose (the Storage Access Framework); false if it could not. */
    suspend fun save(context: Context, image: Bitmap, uri: Uri): Boolean =
        SharedFiles.save(context, uri) { ShareImage.write(image, it) }
}
