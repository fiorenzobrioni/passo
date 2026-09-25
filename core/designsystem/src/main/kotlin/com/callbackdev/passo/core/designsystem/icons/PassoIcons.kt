package com.callbackdev.passo.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's line icons, drawn here on a 24-unit grid: one 1.8 stroke, round caps and joins, the
 * weight of Material Symbols' outlined set, so they sit beside Material's own components. Drawn
 * rather than taken from a library: a dozen shapes did not justify a new dependency, and every
 * one of them is geometry a reader can check. `Icon` tints them like any other vector.
 */
object PassoIcons {
    val Settings: ImageVector by lazy {
        icon("settings") {
            // A gear of eight teeth around a hub: points computed, not traced.
            val teeth = 8
            for (i in 0 until teeth * 4) {
                val angle = 2 * PI * i / (teeth * 4) - PI / 2
                val radius = if ((i / 2) % 2 == 0) 9.6 else 7.4
                val x = (12 + radius * cos(angle)).toFloat()
                val y = (12 + radius * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
            circle(12f, 12f, 3f)
        }
    }

    val Back: ImageVector by lazy {
        icon("back", autoMirror = true) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("chevron_right", autoMirror = true) {
            moveTo(9.5f, 6f)
            lineTo(15.5f, 12f)
            lineTo(9.5f, 18f)
        }
    }

    val Check: ImageVector by lazy {
        icon("check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 7f)
        }
    }

    val Pause: ImageVector by lazy {
        icon("pause") {
            moveTo(9f, 6f)
            lineTo(9f, 18f)
            moveTo(15f, 6f)
            lineTo(15f, 18f)
        }
    }

    val Play: ImageVector by lazy {
        icon("play") {
            moveTo(8f, 5.5f)
            lineTo(18.5f, 12f)
            lineTo(8f, 18.5f)
            close()
        }
    }

    val Info: ImageVector by lazy {
        icon("info") {
            circle(12f, 12f, 9f)
            moveTo(12f, 11f)
            lineTo(12f, 16.5f)
            moveTo(12f, 7.6f)
            lineTo(12f, 7.7f)
        }
    }

    val Warning: ImageVector by lazy {
        icon("warning") {
            moveTo(12f, 3.8f)
            lineTo(21f, 19.8f)
            lineTo(3f, 19.8f)
            close()
            moveTo(12f, 9.5f)
            lineTo(12f, 14f)
            moveTo(12f, 16.9f)
            lineTo(12f, 17f)
        }
    }

    /** A shield: the privacy statement. */
    val Shield: ImageVector by lazy {
        icon("shield") {
            moveTo(12f, 3f)
            lineTo(19f, 6f)
            lineTo(19f, 11f)
            curveTo(19f, 15.5f, 16f, 19f, 12f, 21f)
            curveTo(8f, 19f, 5f, 15.5f, 5f, 11f)
            lineTo(5f, 6f)
            close()
            moveTo(9f, 12f)
            lineTo(11.2f, 14.2f)
            lineTo(15.2f, 10f)
        }
    }

    /** A route between two pins: distance. */
    val Distance: ImageVector by lazy {
        icon("distance") {
            circle(6f, 18f, 2f)
            circle(18f, 6f, 2f)
            moveTo(7.8f, 16.6f)
            curveTo(10f, 14f, 8f, 11.5f, 11.5f, 11.5f)
            curveTo(15f, 11.5f, 14f, 9.5f, 16.3f, 7.3f)
        }
    }

    /** A flame: active calories. */
    val Flame: ImageVector by lazy {
        icon("flame") {
            moveTo(12f, 3f)
            curveTo(12.5f, 6.5f, 17.5f, 8.5f, 17.5f, 14f)
            curveTo(17.5f, 17.6f, 15f, 20.5f, 12f, 20.5f)
            curveTo(9f, 20.5f, 6.5f, 17.6f, 6.5f, 14.5f)
            curveTo(6.5f, 11.5f, 8.5f, 10.5f, 9f, 8f)
            curveTo(10.5f, 9.5f, 10.8f, 11f, 10.8f, 12f)
            curveTo(12f, 10f, 12.5f, 6.5f, 12f, 3f)
            close()
        }
    }

    /** A clock: active minutes. */
    val Clock: ImageVector by lazy {
        icon("clock") {
            circle(12f, 12f, 9f)
            moveTo(12f, 7f)
            lineTo(12f, 12f)
            lineTo(15.5f, 14f)
        }
    }

    /** A bolt: brisk minutes. */
    val Bolt: ImageVector by lazy {
        icon("bolt") {
            moveTo(13f, 2.5f)
            lineTo(5.5f, 13.5f)
            lineTo(11.5f, 13.5f)
            lineTo(10.5f, 21.5f)
            lineTo(18.5f, 10f)
            lineTo(12.5f, 10f)
            close()
        }
    }

    /** A pulse: cadence. */
    val Pulse: ImageVector by lazy {
        icon("pulse") {
            moveTo(3f, 12f)
            lineTo(7f, 12f)
            lineTo(9.5f, 6f)
            lineTo(14f, 18f)
            lineTo(16.5f, 12f)
            lineTo(21f, 12f)
        }
    }

    /** Two footprints: steps, and the app itself. */
    val Steps: ImageVector by lazy {
        icon("steps") {
            ellipse(7.8f, 11.5f, 3f, 4.6f)
            circle(7.8f, 19f, 1.8f)
            ellipse(16.2f, 6.5f, 3f, 4.6f)
            circle(16.2f, 14f, 1.8f)
        }
    }

    val Person: ImageVector by lazy {
        icon("person") {
            circle(12f, 8f, 3.6f)
            moveTo(5f, 20f)
            curveTo(5f, 16.2f, 8.2f, 14f, 12f, 14f)
            curveTo(15.8f, 14f, 19f, 16.2f, 19f, 20f)
        }
    }

    /** A flag on a pole: the goal. */
    val Flag: ImageVector by lazy {
        icon("flag") {
            moveTo(6f, 21f)
            lineTo(6f, 4f)
            moveTo(6f, 4.5f)
            lineTo(18f, 4.5f)
            lineTo(15.5f, 8.5f)
            lineTo(18f, 12.5f)
            lineTo(6f, 12.5f)
        }
    }

    val Bell: ImageVector by lazy {
        icon("bell") {
            moveTo(6f, 16.5f)
            lineTo(6f, 11f)
            curveTo(6f, 7.5f, 8.7f, 5f, 12f, 5f)
            curveTo(15.3f, 5f, 18f, 7.5f, 18f, 11f)
            lineTo(18f, 16.5f)
            lineTo(19.5f, 18f)
            lineTo(4.5f, 18f)
            close()
            moveTo(10f, 20.5f)
            curveTo(10.4f, 21.3f, 11.1f, 21.7f, 12f, 21.7f)
            curveTo(12.9f, 21.7f, 13.6f, 21.3f, 14f, 20.5f)
        }
    }

    /** A battery: the manufacturer's battery settings. */
    val Battery: ImageVector by lazy {
        icon("battery") {
            moveTo(5f, 7f)
            lineTo(17f, 7f)
            curveTo(18.1f, 7f, 19f, 7.9f, 19f, 9f)
            lineTo(19f, 15f)
            curveTo(19f, 16.1f, 18.1f, 17f, 17f, 17f)
            lineTo(5f, 17f)
            curveTo(3.9f, 17f, 3f, 16.1f, 3f, 15f)
            lineTo(3f, 9f)
            curveTo(3f, 7.9f, 3.9f, 7f, 5f, 7f)
            close()
            moveTo(21.5f, 10.5f)
            lineTo(21.5f, 13.5f)
            moveTo(11.5f, 9f)
            lineTo(8.5f, 12.3f)
            lineTo(12.5f, 12.3f)
            lineTo(9.5f, 15f)
        }
    }

    val Plus: ImageVector by lazy {
        icon("plus") {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    val Minus: ImageVector by lazy {
        icon("minus") {
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    /** An arrow up and to the right: ahead of a usual day. */
    val TrendUp: ImageVector by lazy {
        icon("trend_up") {
            moveTo(4f, 17f)
            lineTo(10f, 11f)
            lineTo(13.5f, 14.5f)
            lineTo(20f, 8f)
            moveTo(15f, 8f)
            lineTo(20f, 8f)
            lineTo(20f, 13f)
        }
    }

    /** An arrow down and to the right: behind a usual day. */
    val TrendDown: ImageVector by lazy {
        icon("trend_down") {
            moveTo(4f, 7f)
            lineTo(10f, 13f)
            lineTo(13.5f, 9.5f)
            lineTo(20f, 16f)
            moveTo(15f, 16f)
            lineTo(20f, 16f)
            lineTo(20f, 11f)
        }
    }

    /** A level line: on a usual day's pace. */
    val TrendFlat: ImageVector by lazy {
        icon("trend_flat") {
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(15.5f, 7.5f)
            lineTo(20f, 12f)
            lineTo(15.5f, 16.5f)
        }
    }

    private fun icon(name: String, autoMirror: Boolean = false, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "passo_$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        ).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw,
        ).build()

    private const val STROKE = 1.8f
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) = ellipse(cx, cy, r, r)

private fun PathBuilder.ellipse(cx: Float, cy: Float, rx: Float, ry: Float) {
    moveTo(cx - rx, cy)
    arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx + rx, y1 = cy)
    arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx - rx, y1 = cy)
    close()
}
