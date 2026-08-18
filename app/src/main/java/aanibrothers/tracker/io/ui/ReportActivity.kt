package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.ReportImageAdapter
import aanibrothers.tracker.io.databinding.ActivityReportBinding
import aanibrothers.tracker.io.helper.PdfReportBuilder
import aanibrothers.tracker.io.helper.PhotoReportRenderer
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.helper.TinyDB
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a photo report: a title and description the user can edit, a set of
 * captured images, and export to PDF or to a single tall image.
 *
 * The report's images live only in this screen's state — nothing is written
 * until an export is asked for.
 */
class ReportActivity : BaseActivity<ActivityReportBinding>(ActivityReportBinding::inflate) {

    private var imageAdapter: ReportImageAdapter? = null
    private val reportImages = mutableListOf<File>()
    private var preferences: TinyDB? = null

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val paths = result.data
            ?.getStringArrayListExtra(ReportPickerActivity.EXTRA_SELECTED_PATHS)
            .orEmpty()

        val added = paths.map(::File).filter { file ->
            file.exists() && reportImages.none { it.absolutePath == file.absolutePath }
        }
        reportImages.addAll(added)
        binding?.renderImages()
    }

    override fun ActivityReportBinding.initView() {
        updateNavigationBarColor(R.color.colorBackground)
    }

    override fun ActivityReportBinding.initExtra() {
        preferences = TinyDB(this@ReportActivity)

        val now = Date()
        textReportDate.text = SimpleDateFormat("EEE, d/M/yyyy", Locale.getDefault()).format(now)
        textReportTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)

        textReportTitle.text = reportTitle()
        textReportDescription.text = reportDescription()

        imageAdapter = ReportImageAdapter(
            onRemove = { file ->
                reportImages.removeAll { it.absolutePath == file.absolutePath }
                renderImages()
            },
            onClick = { file -> openImage(file) }
        )
        recyclerReportImages.layoutManager = GridLayoutManager(this@ReportActivity, GRID_SPAN)
        recyclerReportImages.adapter = imageAdapter
        renderImages()
    }

    override fun ActivityReportBinding.initListeners() {
        actionBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        actionAddImages.setOnClickListener {
            pickImagesLauncher.launch(Intent(this@ReportActivity, ReportPickerActivity::class.java))
        }

        actionEditTitle.setOnClickListener {
            promptForText(R.string.title_edit_report_title, reportTitle()) { value ->
                preferences?.putString(KEY_TITLE, value)
                textReportTitle.text = value
            }
        }
        textReportTitle.setOnClickListener { actionEditTitle.performClick() }

        actionEditDescription.setOnClickListener {
            promptForText(R.string.title_edit_report_description, reportDescription()) { value ->
                preferences?.putString(KEY_DESCRIPTION, value)
                textReportDescription.text = value
            }
        }
        textReportDescription.setOnClickListener { actionEditDescription.performClick() }

        actionSavePdf.setOnClickListener { exportReport(asPdf = true) }
        actionSavePhoto.setOnClickListener { exportReport(asPdf = false) }
        actionShare.setOnClickListener { exportReport(asPdf = true) }

        actionMyPdfFiles.setOnClickListener {
            startActivity(Intent(this@ReportActivity, MyPdfFilesActivity::class.java))
        }
    }

    private fun ActivityReportBinding.renderImages() {
        imageAdapter?.submit(reportImages.toList())
        layoutEmpty.visibility = if (reportImages.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun reportTitle(): String {
        return preferences?.getString(KEY_TITLE, "").orEmpty()
            .ifBlank { getString(R.string.app_name_splash) }
    }

    private fun reportDescription(): String {
        return preferences?.getString(KEY_DESCRIPTION, "").orEmpty()
            .ifBlank { getString(R.string.capture_photos_with_gps_location_date) }
    }

    /** Small inline editor, so the header text can be changed in place. */
    private fun promptForText(titleRes: Int, current: String, onDone: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(current)
            setSelection(current.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            val padding = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._16sdp)
            setPadding(padding, padding / 2, padding, padding / 2)
        }
        val container = FrameLayout(this).apply { addView(input) }

        MaterialAlertDialogBuilder(this)
            .setTitle(titleRes)
            .setView(container)
            .setPositiveButton(R.string.button_save) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) onDone(value)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun ActivityReportBinding.exportReport(asPdf: Boolean) {
        if (reportImages.isEmpty()) {
            Toast.makeText(
                this@ReportActivity,
                getString(R.string.message_add_images_first),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressReport.visibility = View.VISIBLE
        val title = reportTitle()
        val description = reportDescription()
        val photos = reportImages.toList()

        lifecycleScope.launch {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = if (asPdf) {
                File(reportsDirectory(), "GPSReport_$timeStamp.pdf")
            } else {
                File(reportsDirectory(), "GPSReport_$timeStamp.jpg")
            }

            val created = withContext(Dispatchers.IO) {
                if (asPdf) {
                    PdfReportBuilder.build(photos, title, description, outFile)
                } else {
                    PhotoReportRenderer.render(photos, title, description, outFile)
                }
            }

            progressReport.visibility = View.GONE
            if (created) {
                shareFile(outFile, if (asPdf) MIME_PDF else MIME_IMAGE)
            } else {
                Toast.makeText(
                    this@ReportActivity,
                    getString(R.string.message_report_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun reportsDirectory(): File {
        return File(getExternalFilesDir(null), REPORTS_FOLDER).apply { mkdirs() }
    }

    private fun openImage(file: File) {
        startActivity(
            Intent(this, ViewCollectionActivity::class.java)
                .putExtra(ViewCollectionActivity.EXTRA_FILE_PATH, file.absolutePath)
        )
    }

    private fun shareFile(file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    getString(R.string.action_share_report)
                )
            )
        } catch (exc: Exception) {
            Toast.makeText(this, getString(R.string.message_report_failed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    companion object {
        const val REPORTS_FOLDER = "Reports"

        private const val GRID_SPAN = 3
        private const val KEY_TITLE = "report_title"
        private const val KEY_DESCRIPTION = "report_description"
        private const val MIME_PDF = "application/pdf"
        private const val MIME_IMAGE = "image/jpeg"
    }
}
