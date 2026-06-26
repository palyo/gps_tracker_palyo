package aanibrothers.tracker.io.analytics

import android.os.Bundle
import androidx.core.os.bundleOf

/**
 * All custom events the app can emit, exactly matching the funnel plan.
 *
 * Each subclass owns its event name and parameters. Names ≤32 chars, snake_case,
 * Firebase reserved prefixes (firebase_, google_, ga_) avoided.
 *
 * Conversion candidates (mark in Firebase Console):
 *   OnboardingCompleted, PermissionFlowCompleted, CaptureSucceeded, plus auto ad_revenue.
 */
sealed class AnalyticsEvent(val name: String) {
    abstract fun bundle(): Bundle

    // ---------- Onboarding ----------

    object LanguageView :
        AnalyticsEvent("language_page_viewed") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    data class LanguageSelected(val langCode: String, val source: String) :
        AnalyticsEvent("language_selected") {
        override fun bundle() = bundleOf("lang_code" to langCode, "source" to source)
    }

    data class OnboardingPageViewed(val pageIndex: Int) :
        AnalyticsEvent("onboarding_page_viewed") {
        override fun bundle() = bundleOf("page_index" to pageIndex)
    }

    object OnboardingCompleted : AnalyticsEvent("onboarding_completed") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    // ---------- Permission funnel ----------

    data class PermissionRequested(val permissionType: String, val attempt: Int) :
        AnalyticsEvent("permission_requested") {
        override fun bundle() = bundleOf("permission_type" to permissionType, "attempt" to attempt)
    }

    data class PermissionGranted(val permissionType: String) :
        AnalyticsEvent("permission_granted") {
        override fun bundle() = bundleOf("permission_type" to permissionType)
    }

    data class PermissionDenied(val permissionType: String, val denialCount: Int) :
        AnalyticsEvent("permission_denied") {
        override fun bundle() = bundleOf(
            "permission_type" to permissionType,
            "denial_count" to denialCount
        )
    }

    data class PermissionSettingsOpened(val permissionType: String, val surface: String) :
        AnalyticsEvent("permission_settings_opened") {
        override fun bundle() = bundleOf("permission_type" to permissionType, "surface" to surface)
    }

    data class PermissionFlowCompleted(val set: String) :
        AnalyticsEvent("permission_flow_completed") {
        override fun bundle() = bundleOf("set" to set)
    }

    // ---------- Call-end dialog (post-Home back-press flow) ----------

    object CallEndDialogShown : AnalyticsEvent("call_end_dialog_shown") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    data class CallEndDialogAction(val action: String) :
        AnalyticsEvent("call_end_dialog_action") {
        override fun bundle() = bundleOf("action" to action)
    }

    // ---------- Home / capture ----------

    data class HomeShown(val isFirstSession: Boolean) : AnalyticsEvent("home_shown") {
        override fun bundle() = bundleOf("is_first_session" to isFirstSession)
    }

    data class CaptureModeChanged(val mode: String) : AnalyticsEvent("capture_mode_changed") {
        override fun bundle() = bundleOf("mode" to mode)
    }

    data class CaptureLocationModeChanged(val locationMode: String) :
        AnalyticsEvent("cap_location_mode_changed") {
        override fun bundle() = bundleOf("location_mode" to locationMode)
    }

    data class TemplateChanged(val template: String) : AnalyticsEvent("template_changed") {
        override fun bundle() = bundleOf("template" to template)
    }

    data class CaptureAttempted(
        val mode: String,
        val template: String,
        val locationMode: String,
        val hasTimer: Int
    ) : AnalyticsEvent("capture_attempted") {
        override fun bundle() = bundleOf(
            "mode" to mode,
            "template" to template,
            "location_mode" to locationMode,
            "has_timer" to hasTimer
        )
    }

    data class CaptureSucceeded(
        val mode: String,
        val template: String,
        val fileSizeKb: Long? = null,
        val durationSeconds: Long? = null
    ) : AnalyticsEvent("capture_succeeded") {
        override fun bundle() = bundleOf(
            "mode" to mode,
            "template" to template
        ).apply {
            fileSizeKb?.let { putLong("file_size_kb", it) }
            durationSeconds?.let { putLong("duration_s", it) }
        }
    }

    data class CaptureFailed(val mode: String, val reason: String) :
        AnalyticsEvent("capture_failed") {
        override fun bundle() = bundleOf("mode" to mode, "reason" to reason)
    }

    data class GalleryViewed(val source: String) : AnalyticsEvent("gallery_viewed") {
        override fun bundle() = bundleOf("source" to source)
    }

    // ---------- Tools & secondary screens ----------

    data class ToolOpened(val tool: String) : AnalyticsEvent("tool_opened") {
        override fun bundle() = bundleOf("tool" to tool)
    }

    data class ToolSessionEnded(val tool: String, val durationSeconds: Long) :
        AnalyticsEvent("tool_session_ended") {
        override fun bundle() = bundleOf("tool" to tool, "duration_s" to durationSeconds)
    }

    // ---------- Misc engagement ----------

    data class RateDialogAction(val action: String) : AnalyticsEvent("rate_dialog_action") {
        override fun bundle() = bundleOf("action" to action)
    }

    data class ExitSheetAction(val action: String) : AnalyticsEvent("exit_sheet_action") {
        override fun bundle() = bundleOf("action" to action)
    }

    // ---------- Ads ----------

    /** Fires when a native/interstitial/app-open ad actually rendered (post-frequency-cap). */
    data class AdImpression(val placement: String, val format: String) :
        AnalyticsEvent("ad_impression_custom") {
        override fun bundle() = bundleOf("placement" to placement, "format" to format)
    }

    /** Fires when an ad fails to load. Useful for per-placement fill-rate. */
    data class AdFailedToLoad(val placement: String, val format: String, val errorCode: Int) :
        AnalyticsEvent("ad_failed_to_load") {
        override fun bundle() = bundleOf(
            "placement" to placement,
            "format" to format,
            "error_code" to errorCode
        )
    }

    /** Fires when an ad request was suppressed by the frequency cap. Reveals revenue we left on the table. */
    data class AdCapped(val placement: String) : AnalyticsEvent("ad_capped") {
        override fun bundle() = bundleOf("placement" to placement)
    }
}
