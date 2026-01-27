package aanibrothers.tracker.io.caller

import aanibrothers.tracker.io.R
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import aanibrothers.tracker.io.caller.frags.AlertFragment
import aanibrothers.tracker.io.caller.frags.DataFragment
import aanibrothers.tracker.io.caller.frags.MessageFragment
import aanibrothers.tracker.io.caller.frags.OptionFragment
import aanibrothers.tracker.io.caller.pager.FragmentCallerPager
import aanibrothers.tracker.io.databinding.ActivityAfterCallBinding
import aanibrothers.tracker.io.module.viewMRECBanner
import android.os.Bundle
import coder.apps.space.library.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AfterCallActivity : BaseActivity<ActivityAfterCallBinding>(ActivityAfterCallBinding::inflate) {
    private var pager: FragmentCallerPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }
    override fun ActivityAfterCallBinding.initExtra() {
        viewPager.apply {
            pager = FragmentCallerPager(supportFragmentManager, lifecycle)
            adapter = pager
            pager?.update(mutableListOf<Fragment>().apply {
                add(DataFragment.newInstance())
                add(MessageFragment.newInstance())
                add(AlertFragment.newInstance())
                add(OptionFragment.newInstance())
            })
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.icon = when (position) {
                    0 -> getDrawable(R.drawable.call_all_data)
                    1 -> getDrawable(R.drawable.call_quick_reply)
                    2 -> getDrawable(R.drawable.call_notification)
                    else -> getDrawable(R.drawable.call_more_options)
                }
            }.attach()
        }
    }

    override fun ActivityAfterCallBinding.initListeners() {
    }

    override fun ActivityAfterCallBinding.initView() {
        viewMRECBanner(adNative)
        callStartTimeText.text = "${callTime} - No answer"
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        textDuration.text = "Time: $currentTime"

    }

}
