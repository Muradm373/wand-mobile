package io_developers.sssemil.com.wand.Account;

import android.content.SharedPreferences;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import rx.Observable;

/**
 * Created by emil on 12/05/17.
 */
public class ApiHelper {
    public static final String PREF_TOKEN = "pref_token";
    public static final String PREF_EMAIL = "pref_email";
    public static final String PREF_NAME = "pref_name";

    private static final String SERVER_URL = "http://wand.ddns.net/";

    private Api mApi;

    public ApiHelper() {
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Accept", "application/json");
            }
        };

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SERVER_URL)
                .setRequestInterceptor(requestInterceptor)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        mApi = restAdapter.create(Api.class);
    }

    public Api getApi() {
        return mApi;
    }

    public interface Api {
        @POST("/init.php")
        @FormUrlEncoded
        Observable<String> signup(@Field("email") String email, @Field("password") String password,
                                  @Field("name") String name, @Field("mobile") String mobile);

        @POST("/login.php")
        @FormUrlEncoded
        Observable<String> login(@Field("email") String email, @Field("password") String password);
    }

    public static void logOut(SharedPreferences sharedPreferences){
        sharedPreferences.edit().remove(PREF_EMAIL).remove(PREF_NAME).remove(PREF_TOKEN).apply();
    }
}