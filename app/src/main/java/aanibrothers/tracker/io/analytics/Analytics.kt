package aanibrothers.tracker.io.analytics

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin, single-entry wrapper around FirebaseAnalytics.
 *
 * Rules:
 *  - Call sites never touch FirebaseAnalytics directly. They use Analytics.log(AnalyticsEvent.X(...)).
 *  - No business logic lives here. Bucketing / derivation belongs to the event's bundle() impl.
 *  - Safe to call from any thread; FirebaseAnalytics already enqueues internally.
 */
object Analytics {

    private var fa: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (fa != null) return
        fa = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    fun log(event: AnalyticsEvent) {
        fa?.logEvent(event.name, event.bundle())
    }

    fun setProperty(name: String, value: String?) {
        fa?.setUserProperty(name, value)
    }

    fun setUserId(id: String?) {
        fa?.setUserId(id)
    }

    /** Manual screen-view emission. Lets us decouple "screen seen" from activity lifecycle quirks. */
    fun setScreen(activity: Activity, screenName: String) {
        fa?.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                FirebaseAnalytics.Param.SCREEN_CLASS to activity::class.java.simpleName
            )
        )
    }
}

/**
 * String-constant catalog so user property names live in one place.
 * (Param names are owned by each event's bundle() impl — they don't escape this file.)
 */
object UserProp {
    const val PREFERRED_TEMPLATE = "preferred_template"
    const val PREFERRED_DIRECTORY = "preferred_directory"
    const val HAS_BASE_PERMS = "has_base_perms"
    const val LIFETIME_CAPTURE_BUCKET = "lifetime_capture_bucket"
    const val USER_STAGE = "user_stage"
    const val HAS_SEEN_CALL_END_DIALOG = "has_seen_call_end_dialog"

    /** Onboarding A/B variant: "on" = onboarding shown, "off" = skipped. */
    const val INTRO_VARIANT = "intro_variant"
}

/**
 * Stable slugs for every ad surface in the app.
 * Used as the `placement` param on ad_impression / ad_failed_to_load events.
 * Adding a new ad screen? Add a slug here so revenue dashboards can break it down.
 */
object AdPlacement {
    // Interstitials
    const val SPLASH = "splash_inter"
    const val LANG_FALLBACK = "lang_fallback_inter"
    const val PERM_CONTINUE = "perm_continue_inter"
    const val MAP_BACK = "map_back_inter"
    const val ROUTE_BACK = "route_back_inter"
    const val AREA_BACK = "area_back_inter"
    const val COMPASS_BACK = "compass_back_inter"
    const val SPEED_BACK = "speed_back_inter"
    const val NEAR_BACK = "near_back_inter"
    const val GPS_CAM_BACK = "gps_cam_back_inter"

    // Natives
    const val LANGUAGE = "lang_native"
    const val ONBOARDING_P2 = "onb_p2_native"
    const val PERMISSION = "perm_native"
    const val HOME_CAPTURE_SHEET = "home_capture_sheet_native"
    const val NEAR = "near_native"
    const val TEMPLATES = "templates_native"
    const val COMPASS = "compass_banner"
    const val SPEED = "speed_banner"
    const val GPS_CAMERA = "gps_cam_banner"

    // App open
    const val APP_OPEN = "app_open"

    const val UNKNOWN = "unknown"
}
