package aanibrothers.tracker.io.extension

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.realPath(uri: Uri?): String? {
    var realPath: String? = null
    val documentFile = uri?.let { DocumentFile.fromSingleUri(this, it) }
    if (documentFile != null && documentFile.exists()) {
        realPath = documentFile.uri.path
    }
    return realPath
}

fun getPathFromURI(context: Context, uri: Uri?): String? {
    if (DocumentsContract.isDocumentUri(context, uri)) {
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
        } else if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri =
                ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )
            return getDataColumn(context, contentUri, null, null)
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            var contentUri: Uri? = null
            contentUri = when (type) {
                "image" -> {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                "video" -> {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                "audio" -> {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                else -> {
                    MediaStore.Files.getContentUri("external")
                }
            }
            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])
            return getDataColumn(context, contentUri, selection, selectionArgs)
        }
    } else if ("content".equals(uri?.scheme, ignoreCase = true)) {
        return getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri?.scheme, ignoreCase = true)) {
        return uri?.path
    }
    return null
}

fun getDataColumn(
    context: Context,
    uri: Uri?,
    selection: String?,
    selectionArgs: Array<String>?
): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(
        column
    )
    try {
        cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(columnIndex)
        }
    } finally {
        cursor?.close()
    }
    return null
}

fun isExternalStorageDocument(uri: Uri?): Boolean {
    return "com.android.externalstorage.documents" == uri?.authority
}

fun isDownloadsDocument(uri: Uri?): Boolean {
    return "com.android.providers.downloads.documents" == uri?.authority
}

fun isMediaDocument(uri: Uri?): Boolean {
    return "com.android.providers.media.documents" == uri?.authority
}

private var _savePhotosFolder: String =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()

var savePhotosFolder: String
    get() {
        var path = _savePhotosFolder
        if (!File(path).exists() || !File(path).isDirectory) {
            path =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
            _savePhotosFolder = path
        }
        return path
    }
    set(value) {
        _savePhotosFolder = value
    }


fun getOutputMediaFilePath(isPhoto: Boolean): String {
    val mediaStorageDir = File(savePhotosFolder)

    if (!mediaStorageDir.exists()) {
        if (!mediaStorageDir.mkdirs()) {
            return ""
        }
    }

    val mediaName = getRandomMediaName(isPhoto)
    return if (isPhoto) {
        "${mediaStorageDir.path}/$mediaName.jpg"
    } else {
        "${mediaStorageDir.path}/$mediaName.mp4"
    }
}

fun getOutputMediaFileName(isPhoto: Boolean): String {
    val mediaName = getRandomMediaName(isPhoto)
    return if (isPhoto) {
        "$mediaName.jpg"
    } else {
        "$mediaName.mp4"
    }
}

fun getRandomMediaName(isPhoto: Boolean): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return if (isPhoto) {
        "IMG_$timestamp"
    } else {
        "VID_$timestamp"
    }
}