package aanibrothers.tracker.io.module

import android.content.*
import com.google.gson.*
import okhttp3.*
import okhttp3.logging.*
import retrofit2.*
import retrofit2.adapter.rxjava2.*
import retrofit2.converter.gson.*
import java.util.concurrent.*

val Context.BASE_URL: String get() = "https://manaager.s3.ap-south-1.amazonaws.com"

class ApiClient(val context: Context) {
    private var retrofit: Retrofit? = null
    private val REQUEST_TIMEOUT = 60
    private var okHttpClient: OkHttpClient? = null
    val client: Retrofit?
        get() {
            if (okHttpClient == null) initOkHttp()
            if (retrofit == null) {
                val gson = GsonBuilder()
                    .setLenient()
                    .create()
                retrofit = okHttpClient?.let {
                    Retrofit.Builder()
                        .baseUrl(context.BASE_URL)
                        .client(it)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .build()
                }
            }
            return retrofit
        }

    private fun initOkHttp() {
        val httpClient: OkHttpClient.Builder = OkHttpClient().newBuilder()
            .connectTimeout(REQUEST_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT.toLong(), TimeUnit.SECONDS)
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        httpClient.addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val requestBuilder: Request.Builder = original.newBuilder()
            val request: Request = requestBuilder.build()
            chain.proceed(request)
        })
        okHttpClient = httpClient.build()
    }
}