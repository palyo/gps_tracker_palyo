package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.widgets.*
import android.annotation.*
import android.content.*
import android.graphics.*
import android.media.*
import android.net.*
import android.os.*
import android.view.*
import androidx.activity.*
import androidx.core.content.*
import androidx.core.view.*
import androidx.viewpager2.widget.*
import coder.apps.space.library.base.*
import coder.apps.space.library.extension.*
import kotlinx.coroutines.*
import java.io.*
import java.util.Locale

class ViewCollectionActivity : BaseActivity<ActivityViewCollectionBinding>(ActivityViewCollectionBinding::inflate) {
    private var fileAdapter: FilePagerAdapter? = null
    private var filesList: MutableList<File>? = mutableListOf()
    private var currentPos = 0
    private var isFullScreenView = false
    private var initialFilePath: String? = null
    private var targetDirectory: File? = null
    override fun ActivityViewCollectionBinding.initExtra() {
        initialFilePath = intent?.getStringExtra(EXTRA_FILE_PATH)
        val dirPath = intent?.getStringExtra(EXTRA_DIR)
        targetDirectory = when {
            !dirPath.isNullOrBlank() -> File(dirPath)
            !initialFilePath.isNullOrBlank() -> File(initialFilePath!!).parentFile
            else -> File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                getString(R.string.folder_gps_camera)
            )
        }
        setupAdapter()
        fetchData()
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            val directory = targetDirectory
                ?.takeIf { it.exists() && it.isDirectory }
                ?: File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    getString(R.string.folder_gps_camera)
                )
            filesList = directory.listFiles { file ->
                file.isFile && isImageFile(file)
            }?.toMutableList() ?: mutableListOf()
            filesList?.sortByDescending { it.lastModified() }
            launch(Dispatchers.Main) {
                val targetPath = initialFilePath
                if (!targetPath.isNullOrBlank()) {
                    val index = filesList?.indexOfFirst { it.absolutePath == targetPath } ?: -1
                    if (index >= 0) currentPos = index
                }
                fileAdapter?.items?.clear()
                fileAdapter?.addAll(filesList)
                binding?.viewPager?.setCurrentItem(currentPos, false)
            }
        }
    }

    private fun ActivityViewCollectionBinding.setupAdapter() {
        viewPager.apply {
            fileAdapter = FilePagerAdapter(this@ViewCollectionActivity)
            adapter = fileAdapter
            fileAdapter?.addAll(filesList)

            setPageTransformer(ZoomOutPageTransformer())
            registerOnPageChangeCallback(pagerListener)
        }
    }

    private val pagerListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            currentPos = position
            val file = filesList?.get(position)
            binding?.toolbar?.title = ""
        }
    }

    fun fragmentClicked() {
        isFullScreenView = !isFullScreenView
        checkSystemUI()
    }

    private fun checkSystemUI() {
        if (isFullScreenView) {
            binding?.hideSystemUi()
        } else {
            binding?.showSystemUi()
        }
    }

    private fun ActivityViewCollectionBinding.showSystemUi() {
        layoutToolbar.beVisible()
        buttonActionBar.beVisible()
        @Suppress("DEPRECATION") window?.apply {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun ActivityViewCollectionBinding.hideSystemUi() {
        layoutToolbar.beGone()
        buttonActionBar.beGone()
        @Suppress("DEPRECATION") window?.apply {
            val decorView = window.decorView
            clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE)
        }
    }

    override fun ActivityViewCollectionBinding.initListeners() {
        buttonShare.setOnClickListener {
            // Guard against empty list / out-of-range currentPos. The previous
            // `filesList?.get(currentPos)` only handled null, not bounds, and
            // crashed with IndexOutOfBoundsException when the list was empty.
            val list = filesList
            if (list.isNullOrEmpty()) {
                "buttonShare".log("filesList is null or empty")
                return@setOnClickListener
            }
            if (currentPos !in list.indices) {
                "buttonShare".log("Invalid index: $currentPos, size: ${list.size}")
                return@setOnClickListener
            }
            shareFile(mutableListOf<File>().apply { add(list[currentPos]) })
        }
        buttonDelete.setOnClickListener {
            val list = filesList
            if (list.isNullOrEmpty()) {
                "actionTrashFile".log("filesList is null or empty")
                return@setOnClickListener
            }
            if (currentPos !in list.indices) {
                "actionTrashFile".log("Invalid index: $currentPos, size: ${list.size}")
                return@setOnClickListener
            }
            actionTrashFile(mutableListOf<File>().apply { add(list[currentPos]) })
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun actionTrashFile(selected: MutableList<File>) {
        viewTrashOrDeleteSheet {
            CoroutineScope(Dispatchers.IO).launch {
                selected.forEach { it.deleteRecursively() }
                launch(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        MediaScannerConnection.scanFile(this@ViewCollectionActivity, selected.map { it.path }.toTypedArray(), null, null)
                        fileAdapter?.removePage(currentPos)
                    }
                }
            }
        }
    }

    fun Context.shareFile(files: MutableList<File>) {
        val uris: ArrayList<Uri> = ArrayList()
        files.forEach { file ->
            val uri: Uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)
            uris.add(uri)
        }
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(this, getString(R.string.chooser_share_files)))
        }
    }

    override fun ActivityViewCollectionBinding.initView() {
        if (initialFilePath.isNullOrBlank()) {
            currentPos = intent?.getIntExtra("position", 0) ?: 0
        }
        showSystemUi()
        window?.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK
        }
        layoutToolbar.updatePadding(top = statusBarHeight)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.getDefault())
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
    }

    companion object {
        const val EXTRA_DIR = "extra_dir"
        const val EXTRA_FILE_PATH = "extra_file_path"
    }
}
