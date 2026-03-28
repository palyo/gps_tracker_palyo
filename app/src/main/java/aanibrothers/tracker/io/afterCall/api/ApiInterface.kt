package aanibrothers.tracker.io.afterCall.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiInterface {
    @GET
    fun getDetails(@Url url: String): Call<ResponseCode>

}