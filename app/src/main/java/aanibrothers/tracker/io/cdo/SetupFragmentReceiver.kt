package aanibrothers.tracker.io.cdo

import aanibrothers.tracker.io.cdo.*
import android.content.*
import com.calldorado.Calldorado.setAftercallCustomView

class SetupFragmentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.PHONE_STATE") {
            setAftercallCustomView(context, AfterCallCustomView(context))
        }
    }
}
