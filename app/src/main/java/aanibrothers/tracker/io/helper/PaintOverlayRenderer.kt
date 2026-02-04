package aanibrothers.tracker.io.helper

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.model.OverlayState
import aanibrothers.tracker.io.model.OverlayTemplate
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import coder.apps.space.library.extension.dimen
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(UnstableApi::class)
class PaintOverlayRenderer(
    private val context: Context,
    private val stateProvider: () -> OverlayState,
    private val mapProvider: () -> Bitmap?,
    private val mapViewType: OverlayTemplate
) : CanvasOverlay(true) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(0x3D, 0, 0, 0)
    }
    private val mapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val fontSemiBold by lazy { ResourcesCompat.getFont(context, coder.apps.space.library.R.font.semi_bold) }
    private val fontMedium by lazy { ResourcesCompat.getFont(context, coder.apps.space.library.R.font.medium) }
    private val fontRegular by lazy { ResourcesCompat.getFont(context, coder.apps.space.library.R.font.regular) }
    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        val marginH = context.dimen(com.intuit.sdp.R.dimen._8sdp)
        val marginV = context.dimen(com.intuit.sdp.R.dimen._6sdp)
        val padding = context.dimen(com.intuit.sdp.R.dimen._8sdp)
        val rowGap = context.dimen(com.intuit.sdp.R.dimen._6sdp)
        val mapSize = context.dimen(com.intuit.sdp.R.dimen._80sdp)

        val gap = context.dimen(com.intuit.sdp.R.dimen._8sdp)
        val radius = context.dimen(com.intuit.sdp.R.dimen._8sdp)

        val cardHeight = mapSize
        val cardLeft = marginH
        val cardRight = w - marginH
        val cardBottom = h - marginV
        val cardTop = cardBottom - cardHeight

        val isRounded = mapViewType == OverlayTemplate.CLASSIC
        val useGap = mapViewType != OverlayTemplate.SQUARISE

        val mapRect = RectF(
            cardLeft,
            cardTop,
            cardLeft + mapSize,
            cardTop + mapSize
        )

        val detailsLeft = if (useGap) mapRect.right + gap else mapRect.right
        val detailsRect = RectF(
            detailsLeft,
            cardTop,
            cardRight,
            cardBottom
        )

        if (isRounded) {
            val clipPath = Path().apply {
                addRoundRect(mapRect, radius, radius, Path.Direction.CW)
            }
            canvas.withClip(clipPath) {
                mapProvider()?.let { bmp ->
                    drawBitmap(bmp, null, mapRect, mapPaint)
                }
            }
        } else {
            mapProvider()?.let { bmp ->
                canvas.drawBitmap(bmp, null, mapRect, mapPaint)
            }
        }

        if (isRounded) {
            canvas.drawRoundRect(detailsRect, radius, radius, bgPaint)
        } else {
            canvas.drawRect(detailsRect, bgPaint)
        }

        val state = stateProvider()
        val textLeft = if (useGap) (mapRect.right + padding + gap) else (mapRect.right + padding)
        var y = cardTop + padding + context.dimen(com.intuit.ssp.R.dimen._5ssp)

        val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = context.dimen(com.intuit.ssp.R.dimen._10ssp)
            typeface = fontSemiBold
        }

        val maxTextWidth = cardRight - textLeft - padding
        val addressLines = wrapText(state.address, addressPaint, maxTextWidth)

        for (line in addressLines.take(2)) {
            canvas.drawText(line, textLeft, y, addressPaint)
            y += addressPaint.textSize + rowGap / 2
        }

        drawText(canvas, "Latitude", textLeft, y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontSemiBold)
        drawText(canvas, "Longitude", textLeft + context.dimen(com.intuit.sdp.R.dimen._80sdp), y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontSemiBold)
        y += context.dimen(com.intuit.sdp.R.dimen._4sdp) + rowGap

        drawText(canvas, "%.6f".format(state.lat), textLeft, y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontRegular)
        drawText(canvas, "%.6f".format(state.lng), textLeft + context.dimen(com.intuit.sdp.R.dimen._80sdp), y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontRegular)
        y += context.dimen(com.intuit.sdp.R.dimen._2sdp) + rowGap

        val nowMillis = state.startTimeMillis + (presentationTimeUs / 1000L)
        val (localTime, gmtTime) = formatTime(nowMillis)

        drawText(canvas, "Local: $localTime}", textLeft, y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontMedium)
        drawText(canvas, "Altitude: ${state.altitudeMeters ?: "N/A"}", textLeft + context.dimen(com.intuit.sdp.R.dimen._80sdp), y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontMedium)
        y += context.dimen(com.intuit.sdp.R.dimen._2sdp) + rowGap

        drawText(canvas, "GMT: ${gmtTime}", textLeft, y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontMedium)
        drawText(canvas, state.date, textLeft + context.dimen(com.intuit.sdp.R.dimen._80sdp), y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontMedium)
        y += context.dimen(com.intuit.sdp.R.dimen._2sdp) + rowGap

        drawText(canvas, "Captured by: ${context.getString(R.string.app_name)}", textLeft, y, context.dimen(com.intuit.ssp.R.dimen._6ssp), fontMedium)
    }

    private fun formatTime(nowMillis: Long): Pair<String, String> {
        val local = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(Date(nowMillis))
        val gmtFormat = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
        val gmt = gmtFormat.format(Date(nowMillis))
        return local to gmt
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        typeface: Typeface?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size
            this.typeface = typeface
        }
        canvas.drawText(text, x, y, paint)
    }

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line = ""

        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) <= maxWidth) {
                line = test
            } else {
                if (line.isNotEmpty()) lines.add(line)
                line = word
            }
        }
        if (line.isNotEmpty()) lines.add(line)
        return lines
    }
}
