package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.CollaboratorSummaryData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Carrega os colaboradores permitidos ao Gestor e renova o Firebase token uma vez após 401. */
public final class CollaboratorsRepository {
    public interface ResultCallback {
        void onSuccess(List<CollaboratorSummaryData> collaborators);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<List<CollaboratorSummaryData>>> currentCall;
    private int requestId;

    public CollaboratorsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static CollaboratorsRepository createDefault() {
        return new CollaboratorsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void load(ResultCallback callback) {
        cancel();
        loadWithToken(false, requestId, callback);
    }

    private void loadWithToken(boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getCollaborators("Bearer " + token);
                currentCall.enqueue(new retrofit2.Callback<ApiResponse<List<CollaboratorSummaryData>>>() {
                    @Override public void onResponse(
                            @NonNull Call<ApiResponse<List<CollaboratorSummaryData>>> call,
                            @NonNull Response<ApiResponse<List<CollaboratorSummaryData>>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.errorBody() != null) response.errorBody().close();
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(true, id, callback);
                            return;
                        }
                        ApiResponse<List<CollaboratorSummaryData>> body = response.body();
                        if (!response.isSuccessful() || body == null || !body.isSuccess()
                                || body.getData() == null || body.getData().stream()
                                .anyMatch(item -> item == null || !item.isValid())) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override public void onFailure(
                            @NonNull Call<ApiResponse<List<CollaboratorSummaryData>>> call,
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
