package aanibrothers.tracker.io.module

import android.app.*
import android.content.*
import com.google.android.ump.*
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener

class ConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()
    val isPrivacyOptionsRequired: Boolean get() = consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(activity: Activity, onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                onConsentGatheringCompleteListener.consentGatheringComplete(formError)
            }
        }, { requestConsentError ->
            onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
        })
    }

    fun showPrivacyOptionsForm(activity: Activity, onConsentFormDismissedListener: OnConsentFormDismissedListener) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    companion object {
        @Volatile
        private var instance: ConsentManager? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: ConsentManager(context).also { instance = it }
            }
    }
}