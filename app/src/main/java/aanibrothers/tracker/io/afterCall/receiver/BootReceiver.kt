package aanibrothers.tracker.io.afterCall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import aanibrothers.tracker.io.afterCall.PostCall

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            PostCall.INSTANCE.startPostCall(context)
        }
    }
}
