package aanibrothers.tracker.io.analytics

import android.os.Bundle
import androidx.core.os.bundleOf

/**
 * Complete catalog of custom Firebase Analytics events for the app.
 *
 * Design rules:
 *  - Names are snake_case, <= 40 chars, and avoid the reserved prefixes
 *    firebase_ / google_ / ga_.
 *  - Each subclass owns its event name AND its parameter bundle, so call
 *    sites stay declarative (Analytics.log(AnalyticsEvent.X(...))).
 *  - screen_view is collected automatically by the Firebase SDK per Activity,
 *    so we don't emit it manually here.
 *
 * The catalog is grouped to mirror the user journey:
 *   App/session -> Acquisition (language/onboarding) -> Permission funnel ->
 *   Activation (capture) -> Feature usage (tools) -> Media management ->
 *   Engagement (rate/feedback/share) -> Churn (uninstall) -> Ads.
 *
 * Mark these as Conversions in the Firebase console:
 *   onboarding_completed, permission_flow_completed, capture_succeeded,
 *   route_navigation_started, feedback_submitted.
 */
sealed class AnalyticsEvent(val name: String) {
    abstract fun bundle(): Bundle

    // ============================================================
    // App / session / retention
    // ============================================================

    /** Warm foreground (not the cold start). Drives DAU/retention + open-count cohorts. */
    data class AppForegrounded(val openCount: Int) : AnalyticsEvent("app_foregrounded") {
        override fun bundle() = bundleOf("open_count" to openCount)
    }

    // ============================================================
    // Acquisition — language & onboarding
    // ============================================================

    object SplashView : AnalyticsEvent("splash_page_viewed") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    object LanguageView : AnalyticsEvent("language_page_viewed") {
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

    /** Conversion. */
    object OnboardingCompleted : AnalyticsEvent("onboarding_completed") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    // ============================================================
    // Permission funnel
    // ============================================================

    /** Permission screen was shown. On a fresh install this always fires before Home. */
    object PermissionView : AnalyticsEvent("permission_page_viewed") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

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

    /** Conversion — user cleared the base permission gate. */
    data class PermissionFlowCompleted(val set: String) :
        AnalyticsEvent("permission_flow_completed") {
        override fun bundle() = bundleOf("set" to set)
    }

    /** The "turn on location services" system dialog/prompt was shown. */
    data class LocationServicesPrompt(val surface: String) :
        AnalyticsEvent("location_services_prompt") {
        override fun bundle() = bundleOf("surface" to surface)
    }

    /** Location services came back enabled after a prompt. */
    data class LocationServicesEnabled(val surface: String) :
        AnalyticsEvent("location_services_enabled") {
        override fun bundle() = bundleOf("surface" to surface)
    }

    // ============================================================
    // Call-end dialog (post-Home flow)
    // ============================================================

    object CallEndDialogShown : AnalyticsEvent("call_end_dialog_shown") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    data class CallEndDialogAction(val action: String) :
        AnalyticsEvent("call_end_dialog_action") {
        override fun bundle() = bundleOf("action" to action)
    }

    // ============================================================
    // Activation — Home & capture
    // ============================================================

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

    /** Camera control toggles: flash / timer / sound / focus / lens flip. */
    data class CameraSettingChanged(val setting: String, val value: String) :
        AnalyticsEvent("camera_setting_changed") {
        override fun bundle() = bundleOf("setting" to setting, "value" to value)
    }

    data class CaptureAttempted(
        val mode: String,
        val template: String,
        val locationMode: String,
        val hasTimer: Int,
        val surface: String = "home"
    ) : AnalyticsEvent("capture_attempted") {
        override fun bundle() = bundleOf(
            "mode" to mode,
            "template" to template,
            "location_mode" to locationMode,
            "has_timer" to hasTimer,
            "surface" to surface
        )
    }

    /** Conversion — primary activation event. */
    data class CaptureSucceeded(
        val mode: String,
        val template: String,
        val fileSizeKb: Long? = null,
        val durationSeconds: Long? = null,
        val surface: String = "home"
    ) : AnalyticsEvent("capture_succeeded") {
        override fun bundle() = bundleOf(
            "mode" to mode,
            "template" to template,
            "surface" to surface
        ).apply {
            fileSizeKb?.let { putLong("file_size_kb", it) }
            durationSeconds?.let { putLong("duration_s", it) }
        }
    }

    data class CaptureFailed(val mode: String, val reason: String, val surface: String = "home") :
        AnalyticsEvent("capture_failed") {
        override fun bundle() = bundleOf("mode" to mode, "reason" to reason, "surface" to surface)
    }

    // ============================================================
    // Media management (gallery / collection)
    // ============================================================

    data class GalleryViewed(val source: String) : AnalyticsEvent("gallery_viewed") {
        override fun bundle() = bundleOf("source" to source)
    }

    data class MediaShared(val type: String, val count: Int, val source: String) :
        AnalyticsEvent("media_shared") {
        override fun bundle() =
            bundleOf("type" to type, "count" to count, "source" to source)
    }

    data class MediaDeleted(val type: String, val source: String) :
        AnalyticsEvent("media_deleted") {
        override fun bundle() = bundleOf("type" to type, "source" to source)
    }

    // ============================================================
    // Feature usage — tools & map
    // ============================================================

    data class ToolOpened(val tool: String) : AnalyticsEvent("tool_opened") {
        override fun bundle() = bundleOf("tool" to tool)
    }

    /** Map place search executed. method = text | voice. */
    data class MapSearchUsed(val method: String) : AnalyticsEvent("map_search_used") {
        override fun bundle() = bundleOf("method" to method)
    }

    /** A marker was placed. source = long_press | poi | search. */
    data class MapMarkerAdded(val source: String) : AnalyticsEvent("map_marker_added") {
        override fun bundle() = bundleOf("source" to source)
    }

    data class MapStyleChanged(val style: String) : AnalyticsEvent("map_style_changed") {
        override fun bundle() = bundleOf("style" to style)
    }

    /** A location/coordinate was shared out of the app. */
    data class LocationShared(val source: String) : AnalyticsEvent("location_shared") {
        override fun bundle() = bundleOf("source" to source)
    }

    /** Turn-by-turn / Google Maps navigation was launched. source = map_marker | near. */
    data class NavigationStarted(val source: String) : AnalyticsEvent("navigation_started") {
        override fun bundle() = bundleOf("source" to source)
    }

    /** Conversion — both endpoints set and directions launched. */
    object RouteNavigationStarted : AnalyticsEvent("route_navigation_started") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    data class AreaCalculated(val points: Int, val unit: String) :
        AnalyticsEvent("area_calculated") {
        override fun bundle() = bundleOf("points" to points, "unit" to unit)
    }

    object AreaCleared : AnalyticsEvent("area_cleared") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    data class NearbyCategorySelected(val category: String) :
        AnalyticsEvent("nearby_category_selected") {
        override fun bundle() = bundleOf("category" to category)
    }

    /** A saved/custom GPS location was added or selected. action = add | select_custom | select_current. */
    data class LocationSaved(val action: String) : AnalyticsEvent("location_saved") {
        override fun bundle() = bundleOf("action" to action)
    }

    // ============================================================
    // Engagement — rate / feedback / share / settings
    // ============================================================

    object RateDialogShown : AnalyticsEvent("rate_dialog_shown") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    data class RateStarSelected(val star: Int) : AnalyticsEvent("rate_star_selected") {
        override fun bundle() = bundleOf("star" to star)
    }

    data class RateDialogAction(val action: String) : AnalyticsEvent("rate_dialog_action") {
        override fun bundle() = bundleOf("action" to action)
    }

    object InAppReviewRequested : AnalyticsEvent("in_app_review_requested") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    /** Conversion — feedback email composed/sent. */
    data class FeedbackSubmitted(val category: String) : AnalyticsEvent("feedback_submitted") {
        override fun bundle() = bundleOf("category" to category)
    }

    data class ExitSheetAction(val action: String, val surface: String = "home") :
        AnalyticsEvent("exit_sheet_action") {
        override fun bundle() = bundleOf("action" to action, "surface" to surface)
    }

    data class AppShared(val source: String) : AnalyticsEvent("app_shared") {
        override fun bundle() = bundleOf("source" to source)
    }

    data class PrivacyPolicyOpened(val source: String) : AnalyticsEvent("privacy_policy_opened") {
        override fun bundle() = bundleOf("source" to source)
    }

    object ConsentFormOpened : AnalyticsEvent("consent_form_opened") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    // ============================================================
    // Churn — uninstall flow
    // ============================================================

    object UninstallFlowOpened : AnalyticsEvent("uninstall_flow_opened") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    /**
     * Every button tap inside the uninstall flow.
     *  screen  = "prompt" (ProbActivity) | "reasons" (ProbQuestionActivity)
     *  button  = the tapped view (e.g. "back", "cancel", "home", "primary", "secondary", "uninstall")
     *  action  = semantic outcome ("keep_app" | "proceed_to_reasons" | "uninstall")
     */
    data class UninstallFlowAction(val screen: String, val button: String, val action: String) :
        AnalyticsEvent("uninstall_flow_action") {
        override fun bundle() =
            bundleOf("screen" to screen, "button" to button, "action" to action)
    }

    data class UninstallReasonSelected(val reason: String) :
        AnalyticsEvent("uninstall_reason_selected") {
        override fun bundle() = bundleOf("reason" to reason)
    }

    object UninstallConfirmed : AnalyticsEvent("uninstall_confirmed") {
        override fun bundle(): Bundle = Bundle.EMPTY
    }

    // ============================================================
    // Ads
    // ============================================================

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
