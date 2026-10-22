package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.NotificationData;
import com.example.astro_mobile.data.api.dto.NotificationDetailsData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import java.util.List;
import java.util.function.Function;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class NotificationsRepository {
    public interface OperationCallback<T> {
        void onSuccess(T result);
        void onFailure(FailureKind failure);
    }
    public interface ResultCallback {
        void onSuccess(List<NotificationData> notifications);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<List<NotificationData>>> currentCall;
    private int requestId;
    private Call<?> detailCall;
    private int detailRequestId;

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
        cancelDetail();
        ++requestId;
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
    }

    // Detalhe e leitura não cancelam a consulta da lista e usam a mesma autenticação.
    public void loadDetail(String notificationId, OperationCallback<NotificationDetailsData> callback) {
        cancelDetail();
        executeDetail(token -> api.getNotification(token, notificationId), false,
                detailRequestId, new OperationCallback<NotificationDetailsData>() {
                    @Override public void onSuccess(NotificationDetailsData result) {
                        if (result == null || result.getMessage() == null) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(result);
                    }
                    @Override public void onFailure(FailureKind failure) { callback.onFailure(failure); }
                });
    }

    public void markViewed(String notificationId, OperationCallback<Void> callback) {
        cancelDetail();
        executeDetail(token -> api.markNotificationViewed(token, notificationId), false,
                detailRequestId, callback);
    }

    private <T> void executeDetail(Function<String, Call<ApiResponse<T>>> request,
                                   boolean refresh, int id, OperationCallback<T> callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != detailRequestId) return;
                Call<ApiResponse<T>> call = request.apply("Bearer " + token);
                detailCall = call;
                call.enqueue(new Callback<ApiResponse<T>>() {
                    @Override public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
                        if (id != detailRequestId) return;
                        detailCall = null;
                        // Renova uma vez; persistindo o 401, a interface encerra a sessão.
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else executeDetail(request, true, id, callback);
                            return;
                        }
                        if (response.code() == 403 || response.code() == 404) {
                            callback.onFailure(FailureKind.BUSINESS);
                            return;
                        }
                        ApiResponse<T> body = response.body();
                        if (response.code() != 200 || body == null || !body.isSuccess()) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }
                    @Override public void onFailure(Call<ApiResponse<T>> call, Throwable error) {
                        if (id != detailRequestId) return;
                        detailCall = null;
                        callback.onFailure(ApiFailures.transport(error));
                    }
                });
            }
            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id != detailRequestId) return;
                callback.onFailure(failure == IdTokenProvider.Failure.SESSION ? FailureKind.SESSION
                        : failure == IdTokenProvider.Failure.CONNECTION ? FailureKind.CONNECTION
                        : FailureKind.INTERNAL);
            }
        });
    }

    public void cancelDetail() {
        ++detailRequestId;
        if (detailCall != null) {
            detailCall.cancel();
            detailCall = null;
        }
    }
}
