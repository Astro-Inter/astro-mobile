package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.NotificationData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class NotificationsRepository {
    public interface ResultCallback {
        void onSuccess(List<NotificationData> notifications);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<List<NotificationData>>> currentCall;
    private int requestId;

    public NotificationsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static NotificationsRepository createDefault() {
        return new NotificationsRepository(AstroApiClient.getApi(),
                FirebaseIdTokenProvider.createDefault());
    }

    public void load(ResultCallback callback) {
        cancel();
        loadWithToken(false, requestId, callback);
    }

    private void loadWithToken(boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override
            public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getNotifications("Bearer " + token);
                currentCall.enqueue(new Callback<ApiResponse<List<NotificationData>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<NotificationData>>> call,
                                           Response<ApiResponse<List<NotificationData>>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(true, id, callback);
                            return;
                        }
                        if (response.code() == 403 || response.code() == 404) {
                            callback.onFailure(FailureKind.BUSINESS);
                            return;
                        }
                        ApiResponse<List<NotificationData>> body = response.body();
                        if (response.code() != 200 || body == null || !body.isSuccess()
                                || body.getData() == null || body.getData().stream()
                                .anyMatch(item -> item == null || !item.isValid())) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<NotificationData>>> call, Throwable error) {
                        if (id != requestId) return;
                        currentCall = null;
                        callback.onFailure(ApiFailures.transport(error));
                    }
                });
            }

            @Override
            public void onFailure(IdTokenProvider.Failure failure) {
                if (id != requestId) return;
                callback.onFailure(failure == IdTokenProvider.Failure.SESSION ? FailureKind.SESSION
                        : failure == IdTokenProvider.Failure.CONNECTION ? FailureKind.CONNECTION
                        : FailureKind.INTERNAL);
            }
        });
    }

    public void cancel() {
        ++requestId;
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
    }
}
