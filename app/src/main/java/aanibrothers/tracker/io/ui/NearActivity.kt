package aanibrothers.tracker.io.ui

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.adapter.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import aanibrothers.tracker.io.module.*
import androidx.activity.*
import androidx.recyclerview.widget.*
import coder.apps.space.library.base.*

class NearActivity : BaseActivity<ActivityNearBinding>(ActivityNearBinding::inflate) {

    override fun ActivityNearBinding.initExtra() {
        setupAdapter()
        viewNativeMedium(adNative)
    }

    private fun ActivityNearBinding.setupAdapter() {
        recyclerView.apply {
            val gridLayoutManager = GridLayoutManager(this@NearActivity, 2, GridLayoutManager.VERTICAL, false)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter?.getItemViewType(position)) {
                        1 -> 2
                        else -> 1
                    }
                }
            }

            layoutManager = gridLayoutManager
            adapter = NearLocationAdapter(this@NearActivity) {
                searchPlaceInGoogleMaps(it.title)
            }
        }
    }

    override fun ActivityNearBinding.initListeners() {

    }

    override fun ActivityNearBinding.initView() {
        toolbar.title = getString(R.string.menu_near_by_location)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback {
            viewInterAdWithLogic {
                finish()
            }
        }
    }
}