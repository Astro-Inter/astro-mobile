package com.example.astro_mobile.chat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.ai.AiApiClient;
import com.example.astro_mobile.data.ai.AiChatApi;
import com.example.astro_mobile.data.ai.GoogleCalendarConnectResponse;
import com.example.astro_mobile.data.ai.IdTokenProvider;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** A conexão só começa ao tocar no link; o token fica no header da API. */
final class ChatCalendarLinkHandler {
    private final Context context;
    private final AiChatApi api;
    private final IdTokenProvider tokens;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Call<GoogleCalendarConnectResponse> call;
    private boolean closed;
    private boolean pending;
    private int generation;
    private final Runnable timeout = () -> finish(generation, null, R.string.chat_calendar_timeout);

    ChatCalendarLinkHandler(Context context) {
        this(context, AiApiClient.create(AiChatApi.class), FirebaseIdTokenProvider.createDefault());
    }

    ChatCalendarLinkHandler(Context context, AiChatApi api, IdTokenProvider tokens) {
        this.context = context;
        this.api = api;
        this.tokens = tokens;
    }

    void connect() {
        if (closed || pending) return;
        pending = true;
        int request = ++generation;
        Toast.makeText(context, R.string.chat_calendar_loading, Toast.LENGTH_SHORT).show();
        main.postDelayed(timeout, 150_000);
        authenticate(request, false);
    }

    private boolean active(int request) { return !closed && pending && generation == request; }

    private void authenticate(int request, boolean refreshed) {
        tokens.getToken(refreshed, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                main.post(() -> {
                    if (!active(request)) return;
                    if (token == null || token.isEmpty()) {
                        finish(request, null, R.string.chat_calendar_session_error);
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
                                    finish(request, response.body().getAuthorizationUrl(), R.string.chat_calendar_error);
                                } else {
                                    finish(request, null, response.code() == 401 ? R.string.chat_calendar_session_error
                                            : R.string.chat_calendar_error);
                                }
                            });
                        }

                        @Override public void onFailure(@NonNull Call<GoogleCalendarConnectResponse> source,
                                                        @NonNull Throwable error) {
                            if (!source.isCanceled()) main.post(() -> finish(request, null, R.string.chat_calendar_error));
                        }
                    });
                });
            }

            @Override public void onFailure(IdTokenProvider.Failure failure) {
                main.post(() -> finish(request, null, failure == IdTokenProvider.Failure.SESSION
                        ? R.string.chat_calendar_session_error : R.string.chat_calendar_error));
            }
        });
    }

    private void finish(int request, String url, int errorMessage) {
        if (!active(request)) return;
        pending = false;
        main.removeCallbacks(timeout);
        if (call != null) { call.cancel(); call = null; }
        Uri uri = url == null ? null : Uri.parse(url);
        if (uri != null && "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null && uri.getUserInfo() == null) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE));
                return;
            } catch (ActivityNotFoundException ignored) { }
        }
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
    }

    void close() {
        closed = true;
        pending = false;
        main.removeCallbacks(timeout);
        if (call != null) { call.cancel(); call = null; }
    }
}
