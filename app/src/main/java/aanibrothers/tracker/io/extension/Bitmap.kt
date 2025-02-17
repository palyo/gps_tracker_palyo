package aanibrothers.tracker.io.extension

import android.content.*
import android.graphics.*
import androidx.annotation.*
import androidx.core.content.*
import com.google.android.gms.maps.model.*

fun Context.vectorToBitmap(@DrawableRes vectorResId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(this, vectorResId) ?: return BitmapDescriptorFactory.defaultMarker()

    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun String.applyOpacity(alpha: Float): Int {
    val color = Color.parseColor(this)
    val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
    return Color.argb(alphaInt, Color.red(color), Color.green(color), Color.blue(color))
}