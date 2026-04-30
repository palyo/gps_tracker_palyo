package com.post.call.info.appStartup

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import androidx.startup.Initializer

class WebViewInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        preloadWebView(context)
    }

    private fun preloadWebView(context: Context) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                WebView(context.applicationContext).destroy()
                Log.e(NotificationCompat.CATEGORY_MESSAGE, "WebView preloaded successfully")
            } catch (e: Exception) {
                Log.e(NotificationCompat.CATEGORY_MESSAGE, "WebView preload failed", e)
            }
            return
        }

        Handler(Looper.getMainLooper()).post {
            try {
                WebView(context.applicationContext).destroy()
                Log.e(NotificationCompat.CATEGORY_MESSAGE, "WebView preloaded from handler")
            } catch (e: Exception) {
                Log.e(NotificationCompat.CATEGORY_MESSAGE, "WebView preload failed", e)
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}