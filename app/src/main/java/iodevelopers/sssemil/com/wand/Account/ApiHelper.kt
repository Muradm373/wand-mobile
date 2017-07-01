package iodevelopers.sssemil.com.wand.Account

import android.content.SharedPreferences
import retrofit.RestAdapter
import retrofit.http.Field
import retrofit.http.FormUrlEncoded
import retrofit.http.POST
import rx.Observable

/**
 * Created by emil on 12/05/17.
 */
class ApiHelper {

    val api: Api

    init {
        val requestInterceptor = retrofit.RequestInterceptor {
            request -> request.addHeader("Accept", "application/json")
        }

        val restAdapter = RestAdapter.Builder()
                .setEndpoint(SERVER_URL)
                .setRequestInterceptor(requestInterceptor)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build()

        api = restAdapter.create(Api::class.java)
    }

    interface Api {
        @POST("/init.php")
        @FormUrlEncoded
        fun signup(@Field("email") email: String, @Field("password") password: String,
                   @Field("name") name: String, @Field("mobile") mobile: String): Observable<String>

        @POST("/login.php")
        @FormUrlEncoded
        fun login(@Field("email") email: String, @Field("password") password: String): Observable<String>
    }

    companion object {
        val PREF_TOKEN = "pref_token"
        val PREF_EMAIL = "pref_email"
        val PREF_NAME = "pref_name"

        private val SERVER_URL = "http://wand.ddns.net/"

        fun logOut(sharedPreferences: SharedPreferences?) {
            sharedPreferences!!.edit()
                    .remove(ApiHelper.Companion.PREF_EMAIL)
                    .remove(ApiHelper.Companion.PREF_NAME)
                    .remove(ApiHelper.Companion.PREF_TOKEN).apply()
        }
    }
}