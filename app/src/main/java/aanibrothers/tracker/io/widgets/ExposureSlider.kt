package aanibrothers.tracker.io.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vertical brightness (exposure compensation) readout shown beside the focus
 * ring after a tap-to-focus, the way stock camera apps do it.
 *
 * This view only draws — the drag gesture that changes the value lives in the
 * camera screen, which owns the CameraX exposure range and feeds the result
 * back through [setProgress]. Keeping it dumb means the widget never has to
 * know what the device's compensation range actually is.
 */
class ExposureSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 0f = darkest end of the supported range, 1f = brightest. */
    private var progress = HALF

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = TRACK_ALPHA
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (clamped == progress) return
        progress = clamped
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val centerX = width / 2f
        val knobRadius = width * KNOB_RADIUS_RATIO
        // Inset the track by the sun's full extent so the glyph never clips
        // against the top or bottom edge at the ends of the range.
        val inset = knobRadius * SUN_EXTENT_RATIO
        val top = inset
        val bottom = height - inset

        trackPaint.strokeWidth = width * TRACK_WIDTH_RATIO
        rayPaint.strokeWidth = width * RAY_WIDTH_RATIO

        canvas.drawLine(centerX, top, centerX, bottom, trackPaint)

        // Progress runs bottom-up: dragging towards the top of the screen is
        // brighter, which is what the gesture in the camera screen maps to.
        val knobY = bottom - (bottom - top) * progress
        canvas.drawCircle(centerX, knobY, knobRadius, knobPaint)

        val rayInner = knobRadius * RAY_INNER_RATIO
        val rayOuter = knobRadius * RAY_OUTER_RATIO
        for (i in 0 until RAY_COUNT) {
            val angle = (Math.PI * 2 / RAY_COUNT) * i
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            canvas.drawLine(
                centerX + dx * rayInner,
                knobY + dy * rayInner,
                centerX + dx * rayOuter,
                knobY + dy * rayOuter,
                rayPaint
            )
        }
    }

    private companion object {
        const val HALF = 0.5f
        const val TRACK_ALPHA = 110
        const val TRACK_WIDTH_RATIO = 0.07f
        const val RAY_WIDTH_RATIO = 0.06f
        const val KNOB_RADIUS_RATIO = 0.20f
        const val SUN_EXTENT_RATIO = 2.2f
        const val RAY_INNER_RATIO = 1.5f
        const val RAY_OUTER_RATIO = 2.1f
        const val RAY_COUNT = 8
    }
}
