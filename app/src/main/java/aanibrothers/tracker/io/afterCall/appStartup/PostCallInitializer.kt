package aanibrothers.tracker.io.afterCall.appStartup

import android.content.Context
import androidx.startup.Initializer
import aanibrothers.tracker.io.afterCall.PostCall

class PostCallInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        PostCall.INSTANCE.startPostCall(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}