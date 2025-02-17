package aanibrothers.tracker.io.cdo

import android.content.*
import android.os.*
import android.text.*
import android.util.*
import com.google.firebase.analytics.*

class FirebaseEventBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null && action == "custom_firebase_event") {
            val mFirebaseAnalytics = FirebaseAnalytics.getInstance(context)
            val eventName = intent.getStringExtra("eventName")
            val imageName = intent.getStringExtra("imageName")
            val fullText = intent.getStringExtra("fullText")
            val eventType = intent.getStringExtra("eventType")
            if (!eventName.isNullOrEmpty() && TextUtils.equals("firebase", eventType)) {
                Log.d(
                    TAG, "logging firebase event.. eventName = " + eventName +
                            ", imageName = " + imageName + ", fullText = " + fullText + ", eventType = " + eventType
                )
                var params = intent.getBundleExtra("eventParams")
                if (params == null) {
                    params = Bundle()
                }
                if (imageName != null) {
                    params.putString("image_name", imageName)
                }
                if (fullText != null) {
                    params.putString("full_text", fullText)
                }
                mFirebaseAnalytics.logEvent(eventName, params)
            }
        }
    }

    companion object {
        private const val TAG = "FirebaseEventB"
    }
}