package aanibrothers.tracker.io.helper

import android.app.Activity
import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Edge-to-edge support for API 36.
 *
 * Android 15 began enforcing edge-to-edge for apps that target it, and this app
 * opted out with `windowOptOutEdgeToEdgeEnforcement`. API 36 ignores that flag
 * — the opt-out is gone — so every screen now draws underneath the status and
 * navigation bars and has to inset its own content.
 *
 * Rather than editing every activity, [applyEdgeToEdgeInsets] is called once
 * per activity from the application's lifecycle callbacks. Screens that are
 * meant to be full-bleed, or that already position their own chrome against
 * the bars, declare [EdgeToEdgeHandled] and are left alone.
 */
interface EdgeToEdgeHandled

fun Activity.applyEdgeToEdgeInsets() {
    if (this is EdgeToEdgeHandled) return
    findViewById<View>(android.R.id.content)?.applySystemBarPadding()
}

/**
 * Pads [this] by the system bars and any display cutout.
 *
 * The view's own padding is captured once and the insets are added to it, so
 * repeated dispatches (rotation, keyboard, gesture-nav changes) can't stack up.
 */
fun View.applySystemBarPadding(
    applyTop: Boolean = true,
    applyBottom: Boolean = true,
    applySides: Boolean = true
) {
    val initial = Rect(paddingLeft, paddingTop, paddingRight, paddingBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.setPadding(
            initial.left + if (applySides) bars.left else 0,
            initial.top + if (applyTop) bars.top else 0,
            initial.right + if (applySides) bars.right else 0,
            initial.bottom + if (applyBottom) bars.bottom else 0
        )
        windowInsets
    }

    requestInsetsWhenAttached()
}

/**
 * Insets are only dispatched to attached views; requesting on a view that is
 * not attached yet is silently dropped, which is the usual reason a screen
 * looks correct after a rotation but not on first launch.
 */
private fun View.requestInsetsWhenAttached() {
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}
