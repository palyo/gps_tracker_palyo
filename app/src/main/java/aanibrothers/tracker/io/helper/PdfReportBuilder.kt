package aanibrothers.tracker.io.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders captured photos into a shareable PDF report.
 *
 * Uses the platform's own [PdfDocument] rather than pulling in a PDF library —
 * the report is images on pages with a header, which needs nothing more.
 *
 * Photos are decoded downsampled to roughly the size they are drawn at. The
 * originals are full-resolution captures, and decoding several of those at
 * native size is exactly the kind of thing that runs a phone out of heap.
 */
object PdfReportBuilder {

    // A4 at 72dpi, the unit PdfDocument works in.
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val PHOTOS_PER_PAGE = 2
    private const val PHOTO_GAP = 16f
    private const val FOOTER_HEIGHT = 28f
    private const val CAPTION_HEIGHT = 14f

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 10f
    }
    private val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 8f
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    /**
     * Writes [photos] into [outFile]. Returns false if nothing could be
     * written, in which case a partial file is cleaned up rather than left
     * behind for the share sheet to pick up.
     */
    fun build(
        photos: List<File>,
        title: String,
        description: String,
        outFile: File
    ): Boolean {
        if (photos.isEmpty()) return false

        val document = PdfDocument()
        return try {
            val generatedOn = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale.getDefault())
                .format(Date())

            var index = 0
            var pageNumber = 1
            while (index < photos.size) {
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                val canvas = page.canvas
                var top = MARGIN

                if (pageNumber == 1) {
                    canvas.drawText(title, MARGIN, top + titlePaint.textSize, titlePaint)
                    top += titlePaint.textSize + 6f
                    if (description.isNotBlank()) {
                        top = drawWrapped(canvas, description, MARGIN, top, PAGE_WIDTH - MARGIN * 2)
                        top += 4f
                    }
                    canvas.drawText(
                        "$generatedOn  ·  ${photos.size} photo${if (photos.size == 1) "" else "s"}",
                        MARGIN, top + subtitlePaint.textSize, subtitlePaint
                    )
                    top += subtitlePaint.textSize + 14f
                }

                val available = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT - top
                val slotHeight = available / PHOTOS_PER_PAGE
                val slotWidth = PAGE_WIDTH - MARGIN * 2

                repeat(PHOTOS_PER_PAGE) {
                    if (index < photos.size) {
                        drawPhotoSlot(
                            canvas, photos[index], MARGIN, top, slotWidth, slotHeight - PHOTO_GAP
                        )
                        top += slotHeight
                        index++
                    }
                }

                canvas.drawText(
                    "Page $pageNumber",
                    PAGE_WIDTH - MARGIN - captionPaint.measureText("Page $pageNumber"),
                    PAGE_HEIGHT - MARGIN + 8f,
                    captionPaint
                )

                document.finishPage(page)
                pageNumber++
            }

            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { document.writeTo(it) }
            true
        } catch (exc: Exception) {
            Log.e("PdfReportBuilder", "Report generation failed", exc)
            outFile.delete()
            false
        } finally {
            document.close()
        }
    }

    /** Lays out [text] across as many lines as the width needs. */
    private fun drawWrapped(
        canvas: Canvas, text: String, left: Float, top: Float, maxWidth: Float
    ): Float {
        var lineTop = top
        val line = StringBuilder()
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (subtitlePaint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line.toString(), left, lineTop + subtitlePaint.textSize, subtitlePaint)
                lineTop += subtitlePaint.textSize + 3f
                line.setLength(0)
                line.append(word)
            } else {
                line.setLength(0)
                line.append(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), left, lineTop + subtitlePaint.textSize, subtitlePaint)
            lineTop += subtitlePaint.textSize + 3f
        }
        return lineTop
    }

    /** Draws one photo centred in its slot, aspect preserved, with a caption. */
    private fun drawPhotoSlot(
        canvas: Canvas, photo: File, left: Float, top: Float, slotWidth: Float, slotHeight: Float
    ) {
        val imageHeight = slotHeight - CAPTION_HEIGHT
        if (imageHeight <= 0f) return

        val bitmap = decodeScaled(photo, slotWidth.toInt(), imageHeight.toInt()) ?: return
        try {
            val scale = minOf(slotWidth / bitmap.width, imageHeight / bitmap.height)
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val drawLeft = left + (slotWidth - drawWidth) / 2f
            val destination = Rect(
                drawLeft.toInt(), top.toInt(),
                (drawLeft + drawWidth).toInt(), (top + drawHeight).toInt()
            )

            canvas.drawBitmap(bitmap, null, destination, null)
            canvas.drawRect(destination, framePaint)
            canvas.drawText(
                photo.name, left, top + drawHeight + CAPTION_HEIGHT - 4f, captionPaint
            )
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Decodes no larger than needed for the slot. A full-resolution capture
     * decoded at native size would be tens of megabytes per photo.
     */
    private fun decodeScaled(photo: File, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            // Aim for ~2x the drawn size so the PDF still looks sharp when the
            // reader zooms in, without decoding the whole sensor frame.
            val wantedWidth = targetWidth * 2
            val wantedHeight = targetHeight * 2
            while (bounds.outWidth / (sampleSize * 2) >= wantedWidth &&
                bounds.outHeight / (sampleSize * 2) >= wantedHeight
            ) {
                sampleSize *= 2
            }

            BitmapFactory.decodeFile(
                photo.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )
        } catch (exc: Exception) {
            Log.e("PdfReportBuilder", "Could not decode ${photo.name}", exc)
            null
        }
    }
}
