package com.callbackdev.passo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.roundToInt

/**
 * Draws a card the way a launcher does: Glance composes it into `RemoteViews` for a size, and
 * `RemoteViews.apply` inflates them into ordinary views, which are laid out and drawn here.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
internal fun renderCard(context: Context, size: DpSize, content: @Composable () -> Unit): Bitmap = runBlocking {
    val density = context.resources.displayMetrics.density
    val views = GlanceRemoteViews().compose(context, size) { content() }.remoteViews
    val host = FrameLayout(context)
    val card = views.apply(context, host)
    host.addView(card)
    val w = (size.width.value * density).roundToInt()
    val h = (size.height.value * density).roundToInt()
    host.measure(
        View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
    )
    host.layout(0, 0, w, h)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    host.draw(Canvas(bitmap))
    bitmap
}

/** Cards on a stand-in wallpaper, one per row or several per row, as a home screen shows them. */
internal class HomeBoard(private val context: Context, private val widthDp: Int = 380) {
    private val rows = mutableListOf<List<Pair<DpSize, Bitmap>>>()

    fun row(vararg cards: Pair<DpSize, Bitmap>) = apply { rows += cards.toList() }

    fun draw(dark: Boolean = true): Bitmap {
        val d = context.resources.displayMetrics.density
        val gap = 16 * d
        val height =
            rows.sumOf { row -> row.maxOf { it.first.height.value.toDouble() } }.toFloat() * d + gap * (rows.size + 1)
        val bitmap = Bitmap.createBitmap((widthDp * d).roundToInt(), height.roundToInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                bitmap.width.toFloat(),
                bitmap.height.toFloat(),
                if (dark) {
                    intArrayOf(
                        Color.rgb(20, 24, 34),
                        Color.rgb(46, 38, 60),
                    )
                } else {
                    intArrayOf(Color.rgb(222, 230, 240), Color.rgb(240, 226, 214))
                },
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        var y = gap
        rows.forEach { row ->
            val total = row.sumOf { it.first.width.value.toDouble() }.toFloat() * d + gap * (row.size - 1)
            var x = (bitmap.width - total) / 2
            row.forEach { (size, card) ->
                // The launcher clips the card to its corner radius; a plain View tree here does not.
                val w = size.width.value * d
                val h = size.height.value * d
                val clip = android.graphics.Path().apply {
                    addRoundRect(x, y, x + w, y + h, 24 * d, 24 * d, android.graphics.Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clip)
                canvas.drawBitmap(card, x, y, null)
                canvas.restore()
                x += w + gap
            }
            y += row.maxOf { it.first.height.value } * d + gap
        }
        return bitmap
    }
}

internal fun Bitmap.saveTo(dir: File, name: String) {
    dir.mkdirs()
    File(dir, "$name.png").outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
}

/** The household's reference grants (Chiaro's `PreviewSize`). */
internal object Grants {
    val OneByOne = DpSize(85.dp, 85.dp)
    val TwoByOne = DpSize(159.dp, 85.dp)
    val ThreeByOne = DpSize(250.dp, 85.dp)
    val FourByOne = DpSize(340.dp, 85.dp)
    val TwoByTwo = DpSize(159.dp, 189.dp)
    val ThreeByTwo = DpSize(250.dp, 189.dp)
    val FourByTwo = DpSize(340.dp, 189.dp)
    val FourByThree = DpSize(340.dp, 293.dp)
    val OneByTwo = DpSize(85.dp, 189.dp)
}
