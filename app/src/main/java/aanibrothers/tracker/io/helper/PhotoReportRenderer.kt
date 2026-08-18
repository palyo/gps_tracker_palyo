package aanibrothers.tracker.io.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the report as a single tall image, for sharing somewhere that takes
 * a picture but not a PDF.
 *
 * Photos are stacked at a fixed width and decoded downsampled, so the output
 * stays a predictable size no matter what the source captures weigh.
 */
object PhotoReportRenderer {

    private const val WIDTH = 1080
    private const val MARGIN = 40f
    private const val PHOTO_GAP = 24f
    private const val JPEG_QUALITY = 92

    fun render(photos: List<File>, title: String, description: String, outFile: File): Boolean {
        if (photos.isEmpty()) return false

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 26f
        }

        var canvasResult: Bitmap? = null
        return try {
            val contentWidth = WIDTH - MARGIN * 2
            val decoded = photos.mapNotNull { decodeScaled(it, contentWidth.toInt()) }
            if (decoded.isEmpty()) return false

            val headerHeight = MARGIN + titlePaint.textSize + 12f +
                (if (description.isBlank()) 0f else bodyPaint.textSize + 8f) +
                bodyPaint.textSize + 20f
            val photosHeight = decoded.sumOf { bitmap ->
                (contentWidth / bitmap.width * bitmap.height + PHOTO_GAP).toDouble()
            }.toFloat()

            val output = createBitmap(WIDTH, (headerHeight + photosHeight + MARGIN).toInt())
            canvasResult = output
            val canvas = Canvas(output)
            canvas.drawColor(Color.WHITE)

            var top = MARGIN
            canvas.drawText(title, MARGIN, top + titlePaint.textSize, titlePaint)
            top += titlePaint.textSize + 12f
            if (description.isNotBlank()) {
                canvas.drawText(description, MARGIN, top + bodyPaint.textSize, bodyPaint)
                top += bodyPaint.textSize + 8f
            }
            canvas.drawText(
                SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date()),
                MARGIN, top + bodyPaint.textSize, bodyPaint
            )
            top += bodyPaint.textSize + 20f

            decoded.forEach { bitmap ->
                val scale = contentWidth / bitmap.width
                val drawHeight = bitmap.height * scale
                canvas.drawBitmap(
                    bitmap, null,
                    Rect(
                        MARGIN.toInt(), top.toInt(),
                        (MARGIN + contentWidth).toInt(), (top + drawHeight).toInt()
                    ),
                    null
                )
                top += drawHeight + PHOTO_GAP
                bitmap.recycle()
            }

            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            true
        } catch (exc: Exception) {
            Log.e("PhotoReportRenderer", "Photo report failed", exc)
            outFile.delete()
            false
        } finally {
            canvasResult?.recycle()
        }
    }

    private fun decodeScaled(photo: File, targetWidth: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.absolutePath, bounds)
            if (bounds.outWidth <= 0) return null

            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= targetWidth) sampleSize *= 2

            BitmapFactory.decodeFile(
                photo.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )
        } catch (exc: Exception) {
            Log.e("PhotoReportRenderer", "Could not decode ${photo.name}", exc)
            null
        }
    }
}
