package com.example.astro_mobile.data.ai;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Solicita a autorização do calendário sem reter Views ou abrir o navegador. */
public final class GoogleCalendarRepository {
    private final AiChatApi api;
    private final IdTokenProvider tokens;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Call<GoogleCalendarConnectResponse> call;
    private boolean closed;
    private boolean pending;
    private int generation;
    private ResultCallback callback;
    // Inclui a obtenção/renovação do token antes da chamada HTTP.
    private final Runnable timeout = () -> finish(generation, null, Failure.TIMEOUT);

    public enum Failure { SESSION, UNAVAILABLE, TIMEOUT }
    public interface ResultCallback { void onResult(String authorizationUrl, Failure failure); }

    public static GoogleCalendarRepository createDefault() {
        return new GoogleCalendarRepository(AiApiClient.create(AiChatApi.class), FirebaseIdTokenProvider.createDefault());
    }

    public GoogleCalendarRepository(AiChatApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public boolean connect(ResultCallback callback) {
        if (closed || pending) return false;
        this.callback = callback;
        pending = true;
        int request = ++generation;
        main.postDelayed(timeout, 150_000);
        authenticate(request, false);
        return true;
    }

    private boolean active(int request) { return !closed && pending && generation == request; }

    private void authenticate(int request, boolean refreshed) {
        tokens.getToken(refreshed, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                main.post(() -> {
                    if (!active(request)) return;
                    if (token == null || token.isEmpty()) {
                        finish(request, null, Failure.SESSION);
                        return;
                    }
                    call = api.connectGoogleCalendar("Bearer " + token);
                    call.enqueue(new Callback<GoogleCalendarConnectResponse>() {
                        @Override public void onResponse(@NonNull Call<GoogleCalendarConnectResponse> source,
                                                         @NonNull Response<GoogleCalendarConnectResponse> response) {
                            if (response.errorBody() != null) response.errorBody().close();
                            main.post(() -> {
                                if (!active(request)) return;
                                if (response.code() == 401 && !refreshed) {
                                    authenticate(request, true);
                                } else if (response.isSuccessful() && response.body() != null) {
                                    finish(request, response.body().getAuthorizationUrl(), Failure.UNAVAILABLE);
                                } else {
                                    finish(request, null, response.code() == 401 ? Failure.SESSION
                                            : Failure.UNAVAILABLE);
                                }
                            });
                        }

                        @Override public void onFailure(@NonNull Call<GoogleCalendarConnectResponse> source,
                                                        @NonNull Throwable error) {
                            if (!source.isCanceled()) main.post(() -> finish(request, null, Failure.UNAVAILABLE));
                        }
                    });
                });
            }

            @Override public void onFailure(IdTokenProvider.Failure failure) {
                main.post(() -> finish(request, null, failure == IdTokenProvider.Failure.SESSION
                        ? Failure.SESSION : Failure.UNAVAILABLE));
            }
        });
    }

    private void finish(int request, String url, Failure failure) {
        if (!active(request)) return;
        pending = false;
        main.removeCallbacks(timeout);
        if (call != null) { call.cancel(); call = null; }
        ResultCallback result = callback;
        callback = null;
        result.onResult(url, url == null ? failure : null);
    }

    public void close() {
        closed = true;
        pending = false;
        callback = null;
        main.removeCallbacks(timeout);
        if (call != null) { call.cancel(); call = null; }
    }
}
