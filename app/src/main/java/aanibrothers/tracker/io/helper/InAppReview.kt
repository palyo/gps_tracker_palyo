package aanibrothers.tracker.io.helper

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import aanibrothers.tracker.io.BuildConfig
import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.LayoutSheetRateBinding
import aanibrothers.tracker.io.extension.firebaseASOEvent
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager

fun getTitleRate() = arrayOf(
    "Love Using Our App?",
    "We’re sorry",
    "Not satisfied?",
    "Thanks for your feedback!",
    "“Glad you like it!",
    "Awesome!"
)

fun getSub() = arrayOf(
    "Rate the app using the stars below",
    "Tell us what went wrong so we can improve.",
    "Your feedback helps us fix issues and make the app better.",
    "What can we improve to earn a higher rating?",
    "Tell us what you enjoy and what could be better.",
    "Thanks for loving the app!"
)

fun getIconSub() = arrayOf(
    R.drawable.img_s5,
    R.drawable.img_s1,
    R.drawable.img_s2,
    R.drawable.img_s3,
    R.drawable.img_s4,
    R.drawable.img_s5,
)

fun Activity.viewRateDialog(
    listener: (Boolean) -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    firebaseASOEvent("rate_view")
    var isRating = false
    var ratedInvoked = false
    val dialog = BottomSheetDialog(this, coder.apps.space.library.R.style.Theme_Space_BottomSheetDialogTheme)
    val bindDialog: LayoutSheetRateBinding = LayoutSheetRateBinding.inflate(layoutInflater)
    dialog.setContentView(bindDialog.root)
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(false)
    val params = dialog.window?.attributes

    params?.width = WindowManager.LayoutParams.MATCH_PARENT
    params?.height = WindowManager.LayoutParams.MATCH_PARENT
    params?.gravity = Gravity.BOTTOM
    dialog.window?.attributes = params
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    dialog.window?.apply {
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setDimAmount(.5f)
    }

    bindDialog.apply {
        bindDialog.textPermission.text = getTitleRate()[0]
        bindDialog.textPermissionBody.text = getSub()[0]
        bindDialog.imgMascot.setImageResource(getIconSub()[0])
        ratingBar.setOnRatingChangeListener { ratingBar, rating, fromUser ->
            isRating = true
            bindDialog.textPermission.text = getTitleRate()[ratingBar.rating.toInt()]
            bindDialog.textPermissionBody.text = getSub()[ratingBar.rating.toInt()]
            bindDialog.imgMascot.setImageResource(getIconSub()[ratingBar.rating.toInt()])
            firebaseASOEvent(
                "rate_choose_star", mapOf("star" to ratingBar.rating.toInt())
            )
        }
        buttonReview.setOnClickListener {
            firebaseASOEvent("rate_view_click_rate")
            if (isRating) {
                ratedInvoked = true
                dialog.dismiss()
                listener.invoke(ratingBar.rating >= 4F)
            } else {
                Toast.makeText(this@viewRateDialog, "Please select star", Toast.LENGTH_SHORT).show()
            }
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }
    // Fires only when the dialog ends WITHOUT a rating outcome (cancel button,
    // back press, system dismiss). When `listener` was already invoked via
    // buttonReview, this is suppressed to avoid double-handling.
    dialog.setOnDismissListener {
        if (!ratedInvoked) onDismiss?.invoke()
    }
    if (!isFinishing && !isDestroyed) {
        dialog.show()
    }
}


interface InAppReviewListener {
    fun onComplete()
    fun onFailed()
}

fun Activity.launchInAppReviewFlow(listener: InAppReviewListener?) {
    val reviewManager = if (BuildConfig.DEBUG) {
        FakeReviewManager(this)
    } else {
        ReviewManagerFactory.create(this)
    }
    val reviewFlowRequest = reviewManager.requestReviewFlow()

    reviewFlowRequest.addOnCompleteListener { requestInfo ->
        if (requestInfo.isSuccessful) {
            val reviewInfo = requestInfo.result
            val flow = reviewManager.launchReviewFlow(this, reviewInfo)
            flow.addOnCompleteListener {
                listener?.onComplete()
            }
            flow.addOnFailureListener {
                listener?.onFailed()
            }
        } else {
            listener?.onFailed()
        }
    }
}





