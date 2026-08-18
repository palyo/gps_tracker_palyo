package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.PdfFileAdapter
import aanibrothers.tracker.io.databinding.ActivityMyPdfFilesBinding
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coder.apps.space.library.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Lists the PDF reports this app has generated. */
class MyPdfFilesActivity :
    BaseActivity<ActivityMyPdfFilesBinding>(ActivityMyPdfFilesBinding::inflate) {

    private var pdfAdapter: PdfFileAdapter? = null

    override fun ActivityMyPdfFilesBinding.initView() {
        updateNavigationBarColor(R.color.colorBackground)
    }

    override fun ActivityMyPdfFilesBinding.initExtra() {
        pdfAdapter = PdfFileAdapter(
            onOpen = { file -> openPdf(file) },
            onShare = { file -> sharePdf(file) }
        )
        recyclerPdfFiles.layoutManager = LinearLayoutManager(this@MyPdfFilesActivity)
        recyclerPdfFiles.adapter = pdfAdapter
    }

    override fun ActivityMyPdfFilesBinding.initListeners() {
        actionBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        // Reload on resume so a report generated on the previous screen is here
        // when the user comes back to look for it.
        binding?.loadFiles()
    }

    private fun ActivityMyPdfFilesBinding.loadFiles() {
        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) {
                File(getExternalFilesDir(null), ReportActivity.REPORTS_FOLDER)
                    .listFiles { file -> file.isFile && file.name.endsWith(".pdf", true) }
                    ?.sortedByDescending { it.lastModified() }
                    .orEmpty()
            }
            pdfAdapter?.submit(files)
            textEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        } catch (exc: Exception) {
            // No PDF viewer installed is the usual cause here.
            Toast.makeText(this, getString(R.string.message_no_pdf_viewer), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun sharePdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
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
}
