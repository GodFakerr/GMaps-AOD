package com.naviaod.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.naviaod.app.model.ManeuverType
import com.naviaod.app.util.PreferencesManager

class OledMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val prefs = PreferencesManager(context)

    // Background is 100% Pitch Black (OLED subpixels powered OFF)
    private val bgPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    // High-Contrast Accent Route Path
    private val routePaint = Paint().apply {
        color = prefs.accentColor
        strokeWidth = 14f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    // Secondary / Ambient Roads (Dim Gray, burns ~1% subpixel energy)
    private val roadPaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Vehicle Marker Paint (White with subtle glow)
    private val vehiclePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Upcoming Maneuver Node Paint
    private val nodePaint = Paint().apply {
        color = Color.parseColor("#00E676") // Green node
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#CCCCCC")
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val routePath = Path()
    private val roadPath = Path()

    var currentManeuver: ManeuverType = ManeuverType.TURN_RIGHT
    var nextStreetName: String = "Upcoming Maneuver"

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPaths(w.toFloat(), h.toFloat())
    }

    private fun rebuildPaths(width: Float, height: Float) {
        routePath.reset()
        roadPath.reset()

        val centerX = width / 2f
        val centerY = height / 2f

        // Ambient intersecting roads
        roadPath.moveTo(0f, centerY)
        roadPath.lineTo(width, centerY)

        roadPath.moveTo(centerX - 180f, 0f)
        roadPath.lineTo(centerX - 180f, height)

        roadPath.moveTo(centerX + 180f, 0f)
        roadPath.lineTo(centerX + 180f, height)

        // Simulated high-contrast route path
        routePath.moveTo(centerX, height - 120f)
        routePath.lineTo(centerX, centerY)

        when (currentManeuver) {
            ManeuverType.TURN_LEFT, ManeuverType.SHARP_LEFT, ManeuverType.SLIGHT_LEFT -> {
                routePath.lineTo(centerX - 240f, centerY)
                routePath.lineTo(centerX - 240f, 160f)
            }
            ManeuverType.TURN_RIGHT, ManeuverType.SHARP_RIGHT, ManeuverType.SLIGHT_RIGHT -> {
                routePath.lineTo(centerX + 240f, centerY)
                routePath.lineTo(centerX + 240f, 160f)
            }
            ManeuverType.U_TURN -> {
                routePath.arcTo(centerX - 60f, centerY - 60f, centerX + 60f, centerY + 60f, 0f, -180f, false)
                routePath.lineTo(centerX - 120f, height - 120f)
            }
            else -> {
                // Straight
                routePath.lineTo(centerX, 120f)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. Draw Pure Black Background (0 light emission)
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // 2. Draw Secondary Roads
        canvas.drawPath(roadPath, roadPaint)

        // 3. Draw Active Route Line
        routePaint.color = prefs.accentColor
        canvas.drawPath(routePath, routePaint)

        // 4. Draw Turn Node (Circle at intersection)
        canvas.drawCircle(centerX, centerY, 16f, nodePaint)

        // 5. Draw Vehicle Marker (Bottom center)
        canvas.drawCircle(centerX, height - 120f, 18f, vehiclePaint)
        canvas.drawCircle(centerX, height - 120f, 8f, bgPaint)

        // 6. Draw Street Label Tag
        canvas.drawText(nextStreetName, centerX, 80f, textPaint)
        canvas.drawText("TAP TO RETURN TO HUD", centerX, height - 40f, textPaint.apply { textSize = 26f })
    }

    fun updateManeuver(maneuver: ManeuverType, street: String) {
        currentManeuver = maneuver
        nextStreetName = street
        if (width > 0 && height > 0) {
            rebuildPaths(width.toFloat(), height.toFloat())
            invalidate()
        }
    }
}
