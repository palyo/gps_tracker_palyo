package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.BuildConfig
import aanibrothers.tracker.io.analytics.Analytics
import aanibrothers.tracker.io.analytics.UserProp
import aanibrothers.tracker.io.extension.IS_INTRO_REMOTE_ENABLED
import aanibrothers.tracker.io.extension.RC_KEY_INTRO_ENABLED
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import coder.apps.space.library.helper.TinyDB
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

/**
 * Thin wrapper around Firebase Remote Config.
 *
 * The `intro_enabled` parameter doubles as an A/B Testing experiment knob:
 * Firebase assigns each user a variant and serves a different value. For the
 * experiment to affect first-session onboarding, the value must be fetched and
 * ACTIVATED before the launcher decides where to navigate — see [whenReady].
 *
 * The latest fetched value is also cached into [TinyDB] so later, synchronous
 * navigation checks (isOnboardingEnabled) read it without a round-trip.
 */
object RemoteConfigManager {

    @Volatile
    private var ready = false
    private val waiters = mutableListOf<() -> Unit>()

    fun init(context: Context) {
        val tinyDB = TinyDB(context.applicationContext)
        val remoteConfig = Firebase.remoteConfig

        val settings = remoteConfigSettings {
            // Allow near-instant refetch in debug so experiments can be tested;
            // throttle in release.
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(settings)

        // Default applied before any network value arrives (= control behaviour).
        remoteConfig.setDefaultsAsync(mapOf(RC_KEY_INTRO_ENABLED to true))

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Reading the value AFTER activate() is what enrolls this user
                // in the A/B experiment and logs the variant exposure.
                val introEnabled = remoteConfig.getBoolean(RC_KEY_INTRO_ENABLED)
                tinyDB.putBoolean(IS_INTRO_REMOTE_ENABLED, introEnabled)
                // Tag the user with their variant so home-reach funnels and any
                // other report can be sliced "onboarding on" vs "off".
                Analytics.setProperty(UserProp.INTRO_VARIANT, if (introEnabled) "on" else "off")
                Log.d(TAG, "RemoteConfig activated: $RC_KEY_INTRO_ENABLED = $introEnabled")
            } else {
                Log.w(TAG, "RemoteConfig fetch failed", task.exception)
            }
            markReady()
        }
    }

    /**
     * Runs [action] once the first fetch/activate has completed, or after
     * [timeoutMs] as a safety net (offline / slow network). Always fires on the
     * main thread, exactly once. This is the gate the launcher uses so the
     * onboarding A/B variant is applied before navigating on the first session.
     */
    fun whenReady(timeoutMs: Long = 3000L, action: () -> Unit) {
        if (ready) {
            action()
            return
        }
        var fired = false
        val once = {
            if (!fired) {
                fired = true
                action()
            }
        }
        synchronized(waiters) { waiters.add(once) }
        Handler(Looper.getMainLooper()).postDelayed({ once() }, timeoutMs)
    }

    private fun markReady() {
        ready = true
        val pending = synchronized(waiters) { waiters.toList().also { waiters.clear() } }
        pending.forEach { it() }
    }
}
