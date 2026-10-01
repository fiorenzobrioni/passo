package com.callbackdev.passo.core.domain.ways

import kotlin.math.cos
import kotlin.math.min

/**
 * The map's projection: equirectangular around the frame's middle latitude, which at the size
 * of a way (a thousand kilometres at most, in latitudes between 41° and 46°) keeps shapes as
 * the eye knows them from any atlas. The frame is fitted inside a [width] by [height] box with
 * [inset] on every side, keeping its proportions, and centred; whatever is outside the frame
 * is drawn too, up to the box's edges (the ground behind is cut wider for that).
 */
class WayProjection(private val frame: GeoBox, width: Float, height: Float, inset: Float = 0f) {
    private val xScale = cos(Math.toRadians(frame.middleLatitude))
    private val scale: Double
    private val offsetX: Double
    private val offsetY: Double

    init {
        val frameWidth = (frame.east - frame.west) * xScale
        val frameHeight = frame.north - frame.south
        scale = min((width - 2 * inset) / frameWidth, (height - 2 * inset) / frameHeight)
        offsetX = (width - frameWidth * scale) / 2
        offsetY = (height - frameHeight * scale) / 2
    }

    fun x(longitude: Double): Float = (offsetX + (longitude - frame.west) * xScale * scale).toFloat()

    fun y(latitude: Double): Float = (offsetY + (frame.north - latitude) * scale).toFloat()

    companion object {
        /**
         * The height over width of a map box for [frame]: its own, held between [min] and
         * [max] so that a long, thin way (the Via degli Dei runs north to south, the Camino
         * west to east) still gets a box that sits well on a page.
         */
        fun boxAspect(frame: GeoBox, min: Double, max: Double): Double = frame.aspect.coerceIn(min, max)
    }
}
