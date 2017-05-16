package io_developers.sssemil.com.wand.Account;

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

    private static final String SERVER_URL = "http://10.50.50.118/";
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
        @POST("/wand/init.php")
        @FormUrlEncoded
        Observable<String> signup(@Field("email") String email, @Field("password") String password,
                                  @Field("name") String name, @Field("mobile") String mobile);

        @POST("/wand/login.php")
        @FormUrlEncoded
        Observable<String> login(@Field("email") String email, @Field("password") String password);
    }
}