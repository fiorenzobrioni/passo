package com.callbackdev.passo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.toSize
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Where a map that moves is looked at from (PLANNING.md §11 Phase 16, «RouteMapView»): how far
 * it is zoomed and where it is dragged to. A point drawn at `p` on the whole map is on screen at
 * `p * scale + offset`. At a [scale] of 1 the map is whole and cannot be dragged; zoomed, it is
 * never dragged past its own edges.
 *
 * Its gestures are a piece of their own ([mapGestures]): the maps of the walks walked on the
 * spot are to move the same way.
 */
@Stable
class MapViewport {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Whether the map is looked at closer than whole: then a finger drags it, not the page. */
    val zoomed: Boolean get() = scale > 1f + EPSILON

    /** Zooms by [factor] around [centroid], a point of the screen that stays under the fingers. */
    fun zoomBy(factor: Float, centroid: Offset, size: Size) {
        val target = (scale * factor).coerceIn(1f, MAX_SCALE)
        val applied = target / scale
        set(target, centroid - (centroid - offset) * applied, size)
    }

    fun panBy(delta: Offset, size: Size) {
        set(scale, offset + delta, size)
    }

    /** Sets both, the offset held inside the map's own edges. */
    fun set(scale: Float, offset: Offset, size: Size) {
        val held = scale.coerceIn(1f, MAX_SCALE)
        this.scale = held
        this.offset = Offset(
            offset.x.coerceIn(size.width * (1f - held), 0f),
            offset.y.coerceIn(size.height * (1f - held), 0f),
        )
    }

    /** The whole map again. */
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /** What a double tap at [at] goes to: closer by [DOUBLE_TAP_ZOOM], or back to whole from far in. */
    fun doubleTapTarget(at: Offset): Pair<Float, Offset> {
        if (scale >= DOUBLE_TAP_ZOOM * DOUBLE_TAP_ZOOM - EPSILON) return 1f to Offset.Zero
        val target = (scale * DOUBLE_TAP_ZOOM).coerceAtMost(MAX_SCALE)
        return target to at - (at - offset) * (target / scale)
    }

    companion object {
        /** Eight times: a street corner of a city walked across. Closer, a line of points is only dots. */
        const val MAX_SCALE: Float = 8f
        const val DOUBLE_TAP_ZOOM: Float = 2f
        private const val EPSILON = 0.001f
    }
}

/**
 * The gestures of a map that moves: two fingers pinch to zoom, one drags the map once it is
 * zoomed, a double tap zooms where it lands (and from far in, back to whole), a tap is [onTap].
 * One finger on a whole map is left to the page under it, which scrolls: the map never traps a
 * finger that only meant to go on reading.
 *
 * The double tap glides, unless [reduced] (the reader asked for less motion): then it lands.
 */
fun Modifier.mapGestures(
    viewport: MapViewport,
    scope: CoroutineScope,
    reduced: Boolean,
    onTap: (Offset) -> Unit = {},
): Modifier = this
    .pointerInput(viewport) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                val bounds = size.toSize()
                if (pressed >= 2) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    if (zoom != 1f) viewport.zoomBy(zoom, event.calculateCentroid(useCurrent = false), bounds)
                    if (pan != Offset.Zero) viewport.panBy(pan, bounds)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                } else if (pressed == 1 && viewport.zoomed) {
                    val pan = event.calculatePan()
                    if (pan != Offset.Zero) {
                        viewport.panBy(pan, bounds)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    .pointerInput(viewport, reduced) {
        detectTapGestures(
            onTap = onTap,
            onDoubleTap = { at ->
                val bounds = size.toSize()
                val (scale, offset) = viewport.doubleTapTarget(at)
                if (reduced) {
                    viewport.set(scale, offset, bounds)
                } else {
                    scope.launch { viewport.glideTo(scale, offset, bounds) }
                }
            },
        )
    }

/** Glides to [scale] and [offset] on the app's own spring; for a reader who asked for less motion, [MapViewport.set]. */
suspend fun MapViewport.glideTo(scale: Float, offset: Offset, size: Size) {
    val fromScale = this.scale
    val fromOffset = this.offset
    Animatable(0f, Float.VectorConverter).animateTo(1f, PassoMotion.spatial()) {
        set(
            fromScale + (scale - fromScale) * value,
            fromOffset + (offset - fromOffset) * value,
            size,
        )
    }
}
