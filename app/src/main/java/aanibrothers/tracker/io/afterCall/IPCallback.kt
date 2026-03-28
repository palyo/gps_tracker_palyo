package aanibrothers.tracker.io.afterCall

interface IPCallback {
    fun ipCallback(isSuccess: Boolean, countryCode: String?)
}