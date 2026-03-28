package aanibrothers.tracker.io.afterCall.api

import com.google.gson.annotations.SerializedName

data class ResponseCode(

	@field:SerializedName("YourFuckingTorExit")
	val yourFuckingTorExit: Boolean? = null,

	@field:SerializedName("YourFuckingCountry")
	val yourFuckingCountry: String? = null,

	@field:SerializedName("YourFuckingCountryCode")
	val yourFuckingCountryCode: String? = null,

	@field:SerializedName("YourFuckingISP")
	val yourFuckingISP: String? = null,

	@field:SerializedName("YourFuckingLocation")
	val yourFuckingLocation: String? = null,

	@field:SerializedName("YourFuckingHostname")
	val yourFuckingHostname: String? = null,

	@field:SerializedName("YourFuckingIPAddress")
	val yourFuckingIPAddress: String? = null,

	@field:SerializedName("YourFuckingCity")
	val yourFuckingCity: String? = null
)
