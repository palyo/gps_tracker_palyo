package aanibrothers.tracker.io.caller.frags

import aanibrothers.tracker.io.databinding.FragmentDataBinding
import aanibrothers.tracker.io.ui.AppSettingsActivity
import aanibrothers.tracker.io.ui.AreaCalcActivity
import aanibrothers.tracker.io.ui.CompassActivity
import aanibrothers.tracker.io.ui.DashboardActivity
import aanibrothers.tracker.io.ui.GPSCameraActivity
import aanibrothers.tracker.io.ui.MapActivity
import aanibrothers.tracker.io.ui.NearActivity
import aanibrothers.tracker.io.ui.RouteActivity
import aanibrothers.tracker.io.ui.SpeedViewActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import coder.apps.space.library.base.BaseFragment
import coder.apps.space.library.extension.go
import com.ironsource.adqualitysdk.sdk.i.it
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.collections.flatMap
import kotlin.jvm.java

class DataFragment : BaseFragment<FragmentDataBinding>(FragmentDataBinding::inflate) {

    override fun FragmentDataBinding.viewCreated() {
    }

    override fun FragmentDataBinding.initListeners() {
        activity?.apply context@{
            binding.apply {
                mapBanner.setOnClickListener {
                    goNext()
                }
                actionMap.setOnClickListener {
                    goNext()
                }
                actionVoice.setOnClickListener {
                    goNext()
                }
                actionRouteFinder.setOnClickListener {
                    goNext()
                }
                actionSpeedometer.setOnClickListener {
                    goNext()
                }
                actionCompass.setOnClickListener {
                    goNext()
                }
                actionNear.setOnClickListener {
                    goNext()
                }
                actionArea.setOnClickListener {
                    goNext()
                }
                actionGpsCamera.setOnClickListener {
                    goNext()
                }
            }
        }
    }

    override fun FragmentDataBinding.initView() {
    }

    override fun create() {
    }

    companion object {
        private const val TAG = "DataFragment"
        fun newInstance() = DataFragment().apply {
            arguments = Bundle().apply {}
        }
    }

    private fun goNext() {
        Intent(requireActivity(), DashboardActivity::class.java).apply {
            startActivity(this)
            activity?.finish()
        }
    }

}
