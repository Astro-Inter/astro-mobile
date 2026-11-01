package com.example.astro_mobile.data.api;

import com.example.astro_mobile.BuildConfig;
import com.example.astro_mobile.data.firebase.AuthFailureKind;
import com.google.gson.Gson;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class PasswordResetRepository {
    public interface ResultCallback {
        void onSuccess();
        void onFailure(AuthFailureKind failure);
        default void onFailure(AuthFailureKind failure, String requestId) { onFailure(failure); }
    }

    private static volatile long retryAt = monotonicMillis();
    private final PasswordResetApi api;

    public PasswordResetRepository(PasswordResetApi api) { this.api = api; }

    public static PasswordResetRepository createDefault() {
        // O serviço de recuperação tem base e cliente próprios, sem token da API principal.
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(70, TimeUnit.SECONDS)
                .readTimeout(70, TimeUnit.SECONDS)
                .writeTimeout(70, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        PasswordResetApi api = new Retrofit.Builder().baseUrl(BuildConfig.PASSWORD_RESET_API_BASE_URL)
                .client(client).addConverterFactory(GsonConverterFactory.create()).build()
                .create(PasswordResetApi.class);
        return new PasswordResetRepository(api);
    }

    public long getRetrySeconds() {
        return Math.max(0, (retryAt - monotonicMillis() + 999) / 1000);
    }

    public void request(String email, ResultCallback callback) {
        if (getRetrySeconds() > 0) {
            callback.onFailure(AuthFailureKind.RATE_LIMIT);
            return;
        }
        api.requestReset(new PasswordResetApi.ResetRequest(email)).enqueue(new Callback<PasswordResetApi.ResetResponse>() {
            @Override public void onResponse(Call<PasswordResetApi.ResetResponse> call,
                                             Response<PasswordResetApi.ResetResponse> response) {
                if (response.code() == 200 && response.body() != null && response.body().success) {
                    callback.onSuccess();
                    return;
                }
                String requestId = safeRequestId(response.headers().get("X-Request-ID"));
                if (response.code() == 429) {
                    retryAt = monotonicMillis() + retryDelayMillis(response.headers().get("Retry-After"));
                    close(response.errorBody());
                    callback.onFailure(AuthFailureKind.RATE_LIMIT, requestId);
                    return;
                }
                String code = errorCode(response.errorBody());
                callback.onFailure(classify(response.code(), code), requestId);
            }

            @Override public void onFailure(Call<PasswordResetApi.ResetResponse> call, Throwable error) {
                callback.onFailure(error instanceof java.io.InterruptedIOException
                        ? AuthFailureKind.RESET_UNCERTAIN
                        : ApiFailures.transport(error) == FailureKind.CONNECTION
                        ? AuthFailureKind.CONNECTION : AuthFailureKind.RESET_UNCERTAIN);
            }
        });
    }

    private static AuthFailureKind classify(int status, String code) {
        if (status == 400 && "INVALID_EMAIL".equals(code)) return AuthFailureKind.INVALID_CREDENTIALS;
        if (status != 503 || code == null) return AuthFailureKind.RESET_UNAVAILABLE;
        switch (code) {
            case "FIREBASE_CONFIGURATION_ERROR":
            case "FIREBASE_AUTH_UNAVAILABLE":
            case "EMAIL_CONFIGURATION_ERROR":
                return AuthFailureKind.RESET_SUPPORT;
            case "EMAIL_DELIVERY_UNAVAILABLE":
            case "PROVIDER_TIMEOUT":
                return AuthFailureKind.RESET_UNCERTAIN;
            case "FIREBASE_UNAVAILABLE":
            case "PROVIDER_UNAVAILABLE":
            case "SERVICE_UNAVAILABLE":
            default:
                return AuthFailureKind.RESET_UNAVAILABLE;
        }
    }

    private static String safeRequestId(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]{1,80}") ? id : null;
    }

    private static long retryDelayMillis(String header) {
        if (header == null) return 60_000L;
        try {
            return Math.max(0, TimeUnit.SECONDS.toMillis(Long.parseLong(header.trim())));
        } catch (NumberFormatException ignored) {
            try {
                long until = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
                return Math.max(0, until - System.currentTimeMillis());
            } catch (DateTimeParseException invalid) {
                return 60_000L;
            }
        }
    }

    private static long monotonicMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private static String errorCode(ResponseBody body) {
        if (body == null) return null;
        try (ResponseBody error = body) {
            PasswordResetApi.ResetResponse parsed = new Gson().fromJson(error.charStream(), PasswordResetApi.ResetResponse.class);
            return parsed == null || parsed.error == null ? null : parsed.error.code;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void close(ResponseBody body) {
        if (body != null) body.close();
    }
}
