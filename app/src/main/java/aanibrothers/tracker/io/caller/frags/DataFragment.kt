package aanibrothers.tracker.io.caller.frags

import aanibrothers.tracker.io.databinding.FragmentDataBinding
import aanibrothers.tracker.io.ui.ToolsActivity
import android.content.Intent
import android.os.Bundle
import coder.apps.space.library.base.BaseFragment
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
        Intent(requireActivity(), ToolsActivity::class.java).apply {
            startActivity(this)
            activity?.finish()
        }
    }
}
