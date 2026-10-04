package com.example.astro_mobile.data.ai;

import com.example.astro_mobile.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Cliente separado da API de contas: a IA pode levar mais tempo para responder. */
public final class AiApiClient {
    private static final Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl(BuildConfig.AI_API_BASE_URL)
            .client(new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .callTimeout(120, TimeUnit.SECONDS)
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private AiApiClient() { }

    public static <T> T create(Class<T> service) {
        return RETROFIT.create(service);
    }
}
