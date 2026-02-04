package aanibrothers.tracker.io.model

import android.graphics.Bitmap

data class OverlayFrame(
    val bitmap: Bitmap,
    val timeMs: Long
    )