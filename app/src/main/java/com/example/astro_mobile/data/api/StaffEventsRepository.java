package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.CollaboratorStaffEventsData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Busca os eventos do colaborador selecionado usando a sessão Firebase atual. */
public final class StaffEventsRepository {
    public interface ResultCallback {
        void onSuccess(CollaboratorStaffEventsData data);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<CollaboratorStaffEventsData>> currentCall;
    private int requestId;

    public StaffEventsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static StaffEventsRepository createDefault() {
        return new StaffEventsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void load(long collaboratorId, ResultCallback callback) {
        cancel();
        loadWithToken(collaboratorId, false, requestId, callback);
    }

    private void loadWithToken(long collaboratorId, boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getStaffEvents("Bearer " + token, collaboratorId);
                currentCall.enqueue(new Callback<ApiResponse<CollaboratorStaffEventsData>>() {
                    @Override public void onResponse(
                            @NonNull Call<ApiResponse<CollaboratorStaffEventsData>> call,
                            @NonNull Response<ApiResponse<CollaboratorStaffEventsData>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.errorBody() != null) response.errorBody().close();
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(collaboratorId, true, id, callback);
                            return;
                        }
                        ApiResponse<CollaboratorStaffEventsData> body = response.body();
                        if (response.code() != 200 || body == null || !body.isSuccess()
                                || body.getData() == null || !body.getData().isValidFor(collaboratorId)) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override public void onFailure(
                            @NonNull Call<ApiResponse<CollaboratorStaffEventsData>> call,
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
