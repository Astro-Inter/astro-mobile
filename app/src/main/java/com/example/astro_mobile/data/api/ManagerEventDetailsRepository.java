package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.ManagerEventDetailsData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import retrofit2.Call;
import retrofit2.Response;

/** Consulta o evento autorizado e renova o token uma vez após 401. */
public final class ManagerEventDetailsRepository {
    public interface ResultCallback {
        void onSuccess(ManagerEventDetailsData profile);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<ManagerEventDetailsData>> currentCall;
    private int requestId;

    public ManagerEventDetailsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static ManagerEventDetailsRepository createDefault() {
        return new ManagerEventDetailsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void load(long eventId, ResultCallback callback) {
        cancel();
        loadWithToken(eventId, false, requestId, callback);
    }

    private void loadWithToken(long eventId, boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getManagerSelfDeclarationDetails("Bearer " + token, eventId);
                currentCall.enqueue(new retrofit2.Callback<ApiResponse<ManagerEventDetailsData>>() {
                    @Override public void onResponse(
                            @NonNull Call<ApiResponse<ManagerEventDetailsData>> call,
                            @NonNull Response<ApiResponse<ManagerEventDetailsData>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.errorBody() != null) response.errorBody().close();
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(eventId, true, id, callback);
                            return;
                        }
                        ApiResponse<ManagerEventDetailsData> body = response.body();
                        if (!response.isSuccessful() || body == null || !body.isSuccess()
                                || body.getData() == null || body.getData().eventId == null
                                || body.getData().eventId != eventId) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override public void onFailure(
                            @NonNull Call<ApiResponse<ManagerEventDetailsData>> call,
                            @NonNull Throwable error) {
                        if (id == requestId && !call.isCanceled()) {
                            currentCall = null;
                            callback.onFailure(ApiFailures.transport(error));
                        }
                    }
                });
            }

            @Override public void onFailure(IdTokenProvider.Failure failure) {
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
