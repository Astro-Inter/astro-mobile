package com.example.astro_mobile.data.api;

import com.example.astro_mobile.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class AstroApiClient {
    public static final long REQUEST_TIMEOUT_SECONDS = 15;
    private static final AstroApi API = createApi();

    private AstroApiClient() {
    }

    public static AstroApi getApi() {
        return API;
    }

    private static AstroApi createApi() {
        // O limite total de 15s evita espera indefinida enquanto o Render inicia.
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AstroApi.class);
    }
}
