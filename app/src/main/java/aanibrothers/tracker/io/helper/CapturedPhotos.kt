package aanibrothers.tracker.io.helper

import aanibrothers.tracker.io.R
import android.content.Context
import android.os.Environment
import java.io.File
import java.util.Locale

/**
 * Finds photos this app captured.
 *
 * Captures do not all land in one place: the default directory setting writes
 * to DCIM/Camera, and the site settings write to Site folders under the app's
 * own directory. Anything that wants to list captures has to sweep all of
 * them, which is why this lives in one place instead of being re-derived per
 * screen.
 *
 * Matching on the capture filename prefix is what keeps DCIM/Camera safe to
 * scan — that folder also holds everything the user shot with the stock
 * camera, and none of that belongs to this app.
 */
object CapturedPhotos {

    private const val CAPTURE_PREFIX = "GPSMapCamera_"

    /** Site folders sit one level below the app directory. */
    private const val MAX_SCAN_DEPTH = 3

    fun findAll(context: Context): List<File> {
        return roots(context)
            .filter { it.exists() && it.isDirectory }
            .flatMap { root -> root.walkTopDown().maxDepth(MAX_SCAN_DEPTH).toList() }
            .filter { it.isFile && it.name.startsWith(CAPTURE_PREFIX) && isImage(it) }
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
    }

    fun latest(context: Context): File? = findAll(context).firstOrNull()

    private fun roots(context: Context): List<File> {
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        return listOf(
            File(dcim, "Camera"),
            File(dcim, context.getString(R.string.location_storage_directory)),
            File(dcim, context.getString(R.string.folder_gps_camera))
        )
    }

    private fun isImage(file: File): Boolean {
        val name = file.name.lowercase(Locale.getDefault())
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
    }
}
