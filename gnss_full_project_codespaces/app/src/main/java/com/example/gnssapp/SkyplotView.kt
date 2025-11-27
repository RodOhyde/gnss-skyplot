package com.example.gnssapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import android.location.GnssStatus

class SkyplotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintCircle = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintSat = Paint().apply {
        style = Paint.Style.FILL
    }

    private val paintText = Paint().apply {
        textSize = 28f
    }

    private var satellites: List<SatelliteData> = emptyList()

    fun updateSatellites(list: List<SatelliteData>) {
        satellites = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - 40

        // Draw main circles (horizon and elevation rings)
        for (i in 1..3) {
            canvas.drawCircle(cx, cy, radius * (i / 3f), paintCircle)
        }

        // Draw satellites
        satellites.forEach { sat ->
            val r = radius * (1f - sat.el / 90f)
            val rad = sat.az * PI / 180f
            val x = cx + r * sin(rad).toFloat()
            val y = cy - r * cos(rad).toFloat()

            paintSat.alpha = if (sat.used) 255 else 120
            paintSat.color = constellationColor(sat.constellation)

            canvas.drawCircle(x, y, 12f, paintSat)
        }

        // Draw simple legend at the top-left
        var oy = 30f
        val spacing = 38f
        val legend = listOf(
            Pair("GPS", constellationColor(GnssStatus.CONSTELLATION_GPS)),
            Pair("GLONASS", constellationColor(GnssStatus.CONSTELLATION_GLONASS)),
            Pair("GALILEO", constellationColor(GnssStatus.CONSTELLATION_GALILEO)),
            Pair("BEIDOU", constellationColor(GnssStatus.CONSTELLATION_BEIDOU)),
            Pair("QZSS", constellationColor(GnssStatus.CONSTELLATION_QZSS)),
            Pair("SBAS", constellationColor(GnssStatus.CONSTELLATION_SBAS)),
            Pair("UNKNOWN", constellationColor(-1))
        )
        legend.forEach { (label, color) ->
            paintSat.color = color
            paintSat.alpha = 255
            canvas.drawCircle(20f, oy, 10f, paintSat)
            canvas.drawText(label, 40f, oy + 8f, paintText)
            oy += spacing
        }
    }

    private fun constellationColor(constellation: Int): Int {
        return when (constellation) {
            GnssStatus.CONSTELLATION_GPS -> 0xFF1E88E5.toInt() // blue
            GnssStatus.CONSTELLATION_GLONASS -> 0xFFD81B60.toInt() // pink
            GnssStatus.CONSTELLATION_BEIDOU -> 0xFF43A047.toInt() // green
            GnssStatus.CONSTELLATION_GALILEO -> 0xFFFDD835.toInt() // yellow
            GnssStatus.CONSTELLATION_QZSS -> 0xFF8E24AA.toInt() // purple
            GnssStatus.CONSTELLATION_SBAS -> 0xFF6D4C41.toInt() // brown
            else -> 0xFF9E9E9E.toInt() // grey
        }
    }
}
