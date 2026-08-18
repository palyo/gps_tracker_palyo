package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.ReportPhotoAdapter
import aanibrothers.tracker.io.databinding.ActivityReportPickerBinding
import aanibrothers.tracker.io.helper.CapturedPhotos
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import coder.apps.space.library.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Picks captured photos to add to a report, returning the chosen paths to
 * [ReportActivity].
 */
class ReportPickerActivity : BaseActivity<ActivityReportPickerBinding>(ActivityReportPickerBinding::inflate) {

    private var photoAdapter: ReportPhotoAdapter? = null

    override fun ActivityReportPickerBinding.initView() {
        updateNavigationBarColor(R.color.colorBlack)
    }

    override fun ActivityReportPickerBinding.initExtra() {
        setupAdapter()
        loadPhotos()
    }

    override fun ActivityReportPickerBinding.initListeners() {
        actionBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        actionSelectAll.setOnClickListener {
            val adapter = photoAdapter ?: return@setOnClickListener
            if (adapter.isAllSelected()) adapter.clearSelection() else adapter.selectAll()
        }

        actionCreateReport.setOnClickListener { returnSelection() }
    }

    private fun ActivityReportPickerBinding.setupAdapter() {
        photoAdapter = ReportPhotoAdapter { selectedCount ->
            actionCreateReport.isEnabled = selectedCount > 0
            actionCreateReport.text = if (selectedCount > 0) {
                getString(R.string.action_add_to_report_count, selectedCount)
            } else {
                getString(R.string.action_add_to_report)
            }
            actionSelectAll.setText(
                if (photoAdapter?.isAllSelected() == true) {
                    R.string.action_clear_selection
                } else {
                    R.string.action_select_all
                }
            )
        }
        recyclerPhotos.layoutManager = GridLayoutManager(this@ReportPickerActivity, GRID_SPAN)
        recyclerPhotos.adapter = photoAdapter
    }

    private fun ActivityReportPickerBinding.loadPhotos() {
        lifecycleScope.launch {
            val photos = withContext(Dispatchers.IO) { CapturedPhotos.findAll(this@ReportPickerActivity) }
            photoAdapter?.submit(photos)
            textEmpty.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** Hands the chosen files back to the report composer. */
    private fun returnSelection() {
        val selected = photoAdapter?.selectedPhotos().orEmpty()
        if (selected.isEmpty()) return

        setResult(
            RESULT_OK,
            Intent().putStringArrayListExtra(
                EXTRA_SELECTED_PATHS, ArrayList(selected.map { it.absolutePath })
            )
        )
        finish()
    }

    companion object {
        const val EXTRA_SELECTED_PATHS = "extra_selected_paths"

        private const val GRID_SPAN = 3
    }
}
