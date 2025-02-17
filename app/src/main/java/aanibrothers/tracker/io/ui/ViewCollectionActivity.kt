package aanibrothers.tracker.io.ui

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

class ViewCollectionActivity : BaseActivity<ActivityViewCollectionBinding>(ActivityViewCollectionBinding::inflate) {
    private var fileAdapter: FilePagerAdapter? = null
    private var filesList: MutableList<File>? = mutableListOf()
    private var currentPos = 0
    private var isFullScreenView = false
    override fun ActivityViewCollectionBinding.initExtra() {
        setupAdapter()
        fetchData()
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            filesList = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPS Camera"
            ).listFiles()?.toMutableList() ?: mutableListOf()
            filesList?.sortByDescending { it.lastModified() }
            launch(Dispatchers.Main) {
                fileAdapter?.addAll(filesList)
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
            binding?.toolbar?.title = file?.nameWithoutExtension
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
            shareFile(mutableListOf<File>().apply {
                filesList?.get(currentPos)?.let { media -> add(media) }
            })
        }
        buttonDelete.setOnClickListener {
            actionTrashFile(mutableListOf<File>().apply {
                filesList?.get(currentPos)?.let { media -> add(media) }
            })
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
            startActivity(Intent.createChooser(this, "Share"))
        }
    }

    override fun ActivityViewCollectionBinding.initView() {
        currentPos = intent?.getIntExtra("position", 0) ?: 0
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
}