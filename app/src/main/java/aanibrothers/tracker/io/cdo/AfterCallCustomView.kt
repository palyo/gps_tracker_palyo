package aanibrothers.tracker.io.cdo

import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.ui.*
import android.content.*
import android.view.*
import com.calldorado.ui.aftercall.*
import com.calldorado.ui.views.custom.*

class AfterCallCustomView(private var mContext: Context) : CalldoradoCustomView(mContext) {
    override fun getRootView(): View {
        val binding = LayoutAftercallNativeBinding.inflate(LayoutInflater.from(mContext), linearViewGroup, false)
        binding.initView()
        return binding.root
    }

    fun LayoutAftercallNativeBinding.initView() {
        actionMap.setOnClickListener {
            goto("map")
        }
        actionCompass.setOnClickListener {
            goto("compass")
        }
        actionSpeedometer.setOnClickListener {
            goto("speedometer")
        }
        actionArea.setOnClickListener {
            goto("area")
        }
        actionNear.setOnClickListener {
            goto("near")
        }
        actionRouteFinder.setOnClickListener {
            goto("route")
        }
    }

    private fun goto(go: String) {
        Intent(mContext, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("mode", "aftercall")
            putExtra("goto", go)
            mContext.startActivity(this)
        }
        if (calldoradoContext is CallerIdActivity) {
            (calldoradoContext as CallerIdActivity).finish()
        }
    }
}