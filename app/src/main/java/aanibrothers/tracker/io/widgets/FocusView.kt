package aanibrothers.tracker.io.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FocusView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var focusShape: Int = FOCUS_SHAPE_CIRCLE // Default to circle shape
    private var focusColor: Int = Color.WHITE
    private var focusSize: Int = DEFAULT_FOCUS_SIZE
    private val paint: Paint = Paint()
    private var isManualFocus = false

    private var colorChangeRunnable: Runnable? = null

    companion object {
        const val FOCUS_SHAPE_SQUARE = 0
        const val FOCUS_SHAPE_CIRCLE = 1
        const val DEFAULT_FOCUS_SIZE = 150 // Default size of the focus icon

        private const val STROKE_WIDTH_DP = 1.5f

        /** Length of each corner bracket arm, as a fraction of the half-size. */
        private const val BRACKET_ARM_RATIO = 0.3f

        /** Length of the ring's cardinal tick marks, as a fraction of the radius. */
        private const val TICK_RATIO = 0.18f
    }

    private val density = resources.displayMetrics.density

    init {
        paint.color = focusColor
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND
        // A hairline reads as a viewfinder reticle; the old fixed 5px line was
        // a heavy ring on low-density screens and a thin one on high-density.
        paint.strokeWidth = STROKE_WIDTH_DP * density
    }

    fun setFocusShape(shape: Int) {
        focusShape = shape
        invalidate()
    }

    fun setFocusColor(color: Int) {
        focusColor = color
        paint.color = focusColor
        invalidate()
    }

    fun setFocusSize(size: Int) {
        focusSize = size
        invalidate()
    }

    fun setManualFocus(isManual: Boolean) {
        isManualFocus = isManual
        // Drop any pending green-lock callback from a previous mode switch,
        // otherwise it fires against the new mode and the ring stays green.
        colorChangeRunnable?.let { removeCallbacks(it) }
        colorChangeRunnable = null

        if (isManualFocus && focusShape == FOCUS_SHAPE_SQUARE) {
            // If manual focus and shape is square, change color to green after 2 seconds
            colorChangeRunnable = Runnable {
                focusColor = Color.GREEN
                paint.color = focusColor
                invalidate()
            }
            postDelayed(colorChangeRunnable, 2000)
        } else {
            focusColor = Color.WHITE
            paint.color = focusColor
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = focusSize / 2f

        if (focusShape == FOCUS_SHAPE_SQUARE && isManualFocus) {
            drawCornerBrackets(canvas, centerX, centerY, radius)
        } else {
            drawReticle(canvas, centerX, centerY, radius)
        }
    }

    /**
     * Manual focus: four corner brackets rather than a closed box, so the
     * subject stays visible inside the frame instead of being boxed in.
     */
    private fun drawCornerBrackets(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val left = centerX - radius
        val top = centerY - radius
        val right = centerX + radius
        val bottom = centerY + radius
        val arm = radius * BRACKET_ARM_RATIO

        canvas.drawLine(left, top, left + arm, top, paint)
        canvas.drawLine(left, top, left, top + arm, paint)

        canvas.drawLine(right - arm, top, right, top, paint)
        canvas.drawLine(right, top, right, top + arm, paint)

        canvas.drawLine(left, bottom - arm, left, bottom, paint)
        canvas.drawLine(left, bottom, left + arm, bottom, paint)

        canvas.drawLine(right - arm, bottom, right, bottom, paint)
        canvas.drawLine(right, bottom - arm, right, bottom, paint)
    }

    /**
     * Auto focus: a thin ring with tick marks at the cardinal points and a
     * centre dot — the viewfinder look, instead of a bare circle.
     */
    private fun drawReticle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        canvas.drawCircle(centerX, centerY, radius, paint)

        val tick = radius * TICK_RATIO
        canvas.drawLine(centerX, centerY - radius, centerX, centerY - radius + tick, paint)
        canvas.drawLine(centerX, centerY + radius - tick, centerX, centerY + radius, paint)
        canvas.drawLine(centerX - radius, centerY, centerX - radius + tick, centerY, paint)
        canvas.drawLine(centerX + radius - tick, centerY, centerX + radius, centerY, paint)

        canvas.drawPoint(centerX, centerY, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        colorChangeRunnable?.let { removeCallbacks(it) }
    }
}