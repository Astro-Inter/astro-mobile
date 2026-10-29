package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.CollaboratorProfileData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import retrofit2.Call;
import retrofit2.Response;

/** Consulta o perfil autorizado e renova o token uma vez após 401. */
public final class CollaboratorProfileRepository {
    public interface ResultCallback {
        void onSuccess(CollaboratorProfileData profile);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<CollaboratorProfileData>> currentCall;
    private int requestId;

    public CollaboratorProfileRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static CollaboratorProfileRepository createDefault() {
        return new CollaboratorProfileRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void load(long collaboratorId, ResultCallback callback) {
        cancel();
        loadWithToken(collaboratorId, false, requestId, callback);
    }

    private void loadWithToken(long collaboratorId, boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getCollaboratorProfile("Bearer " + token, collaboratorId);
                currentCall.enqueue(new retrofit2.Callback<ApiResponse<CollaboratorProfileData>>() {
                    @Override public void onResponse(
                            @NonNull Call<ApiResponse<CollaboratorProfileData>> call,
                            @NonNull Response<ApiResponse<CollaboratorProfileData>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.errorBody() != null) response.errorBody().close();
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(collaboratorId, true, id, callback);
                            return;
                        }
                        ApiResponse<CollaboratorProfileData> body = response.body();
                        if (!response.isSuccessful() || body == null || !body.isSuccess()
                                || body.getData() == null || body.getData().id == null
                                || body.getData().id != collaboratorId) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override public void onFailure(
                            @NonNull Call<ApiResponse<CollaboratorProfileData>> call,
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
