package com.post.call.info.appStartup

import android.content.Context
import androidx.startup.Initializer
import com.post.call.info.service.PostCall

class PostCallInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        PostCall.INSTANCE.startPostCall(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}