package com.callbackdev.passo.widget.glance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * What the ring shows: the share of the goal walked, where a usual day stands by now (the
 * notch, Today's own), and whether the count is live.
 *
 * @property progress not capped: past 1 the ring is simply closed, in the goal's colour.
 * @property usual null hides the notch: no typical day yet, the line turned off, or a met goal.
 */
internal data class RingSpec(val progress: Float, val usual: Float?, val reached: Boolean, val live: Boolean)

/** The ring's colours, from the card's [com.callbackdev.passo.widget.WidgetPalette]. */
internal data class RingInks(
    val accent: Color,
    val quiet: Color,
    val track: Color,
    val goal: Color,
    val notch: Color,
    val halo: Color?,
)

/**
 * Today's ring, painted into a bitmap of exactly the size it is shown at (PLANNING.md §7, the
 * Phase 4 spike; `docs/adr/0005-widgets.md` has the comparison with 21 vector levels).
 *
 * A bitmap because the ring is the one drawing a widget cannot build from boxes: an arc with
 * round caps, in any of the card's inks, with the notch at any minute of the day. Pre-built
 * vector levels would have fixed the progress to 5% steps and the notch to 5% too (a notch that
 * says "usual" 400 steps away from where usual is), and needed a drawable per level and per mark.
 *
 * Kept small on purpose: Android 17 caps the bitmap memory of a `RemoteViews` for apps that
 * target it, and exceeding it is a crash. One ring is `side² × 4` bytes, [MAX_SIDE_PX] caps the
 * side, so the largest ring a card can ask for is 0.7 MB and a one-row ring about 50 KB, one
 * bitmap per card; a sharper ring past that size would not be visible anyway.
 */
internal object RingPainter {
    /** The largest side painted: a 104 dp ring on a 4.0 density screen, where the hero glyphs stop. */
    const val MAX_SIDE_PX: Int = 416

    /** The stroke, as a share of the ring's side: heavier than Today's, because a widget is read from further away. */
    const val STROKE_SHARE: Float = 0.11f

    fun strokeOf(side: Dp): Dp = maxOf(side * STROKE_SHARE, MIN_STROKE)

    fun paint(sideDp: Dp, density: Float, spec: RingSpec, inks: RingInks): Bitmap {
        val side = (sideDp.value * density).roundToInt().coerceIn(1, MAX_SIDE_PX)
        val scale = side / (sideDp.value * density).coerceAtLeast(1f)
        val px = density * scale
        val bitmap = createBitmap(side, side)
        val canvas = Canvas(bitmap)
        val stroke = strokeOf(sideDp).value * px
        val inset = stroke / 2f
        val oval = RectF(inset, inset, side - inset, side - inset)
        val radius = (side - stroke) / 2f
        val center = side / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        paint.color = inks.track.toArgb()
        canvas.drawArc(oval, 0f, 360f, false, paint)

        val share = spec.progress.coerceIn(0f, 1f)
        if (share > 0f) {
            paint.color = when {
                spec.reached -> inks.goal
                spec.live -> inks.accent
                else -> inks.quiet
            }.toArgb()
            paint.strokeCap = if (share >= 1f) Paint.Cap.BUTT else Paint.Cap.ROUND
            canvas.drawArc(oval, -90f, 360f * share, false, paint)
        }

        val usual = spec.usual
        if (usual != null && usual > 0f && usual <= 1f && !spec.reached) {
            val angle = (usual * 360f - 90f) * DEG
            val reach = stroke / 2f + NOTCH_OVERHANG.value * px
            val fromX = center + (radius - reach) * cos(angle)
            val fromY = center + (radius - reach) * sin(angle)
            val toX = center + (radius + min(reach, inset)) * cos(angle)
            val toY = center + (radius + min(reach, inset)) * sin(angle)
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            inks.halo?.let { halo ->
                line.color = halo.toArgb()
                line.strokeWidth = NOTCH_HALO.value * px
                canvas.drawLine(fromX, fromY, toX, toY, line)
            }
            line.color = inks.notch.toArgb()
            line.strokeWidth = NOTCH_WIDTH.value * px
            canvas.drawLine(fromX, fromY, toX, toY, line)
        }
        return bitmap
    }

    private val MIN_STROKE = 3.dp
    private val NOTCH_WIDTH = 2.dp
    private val NOTCH_HALO = 5.dp
    private val NOTCH_OVERHANG = 3.dp
    private const val DEG = (PI / 180).toFloat()
}
