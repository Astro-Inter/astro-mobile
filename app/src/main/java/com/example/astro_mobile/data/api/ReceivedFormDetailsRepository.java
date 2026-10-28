package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;

import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class ReceivedFormDetailsRepository {
    public interface ResultCallback {
        void onSuccess(ReceivedFormDetails details);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<ReceivedFormDetails>> currentCall;
    private int requestId;

    public ReceivedFormDetailsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static ReceivedFormDetailsRepository createDefault() {
        return new ReceivedFormDetailsRepository(AstroApiClient.getApi(),
                FirebaseIdTokenProvider.createDefault());
    }

    public void load(String formId, ResultCallback callback) {
        cancel();
        loadWithToken(formId, false, requestId, callback);
    }

    private void loadWithToken(String formId, boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override
            public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getReceivedForm("Bearer " + token, formId);
                currentCall.enqueue(new Callback<ApiResponse<ReceivedFormDetails>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ReceivedFormDetails>> call,
                                           Response<ApiResponse<ReceivedFormDetails>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        // Renova uma vez para token expirado; falta de permissão não é nova autenticação.
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(formId, true, id, callback);
                            return;
                        }
                        if (response.code() == 403 || response.code() == 404) {
                            callback.onFailure(FailureKind.BUSINESS);
                            return;
                        }
                        ApiResponse<ReceivedFormDetails> body = response.body();
                        if (response.code() != 200 || body == null || !body.isSuccess()
                                || body.getData() == null || body.getData().form == null
                                || !formId.equals(body.getData().form.id) || body.getData().form.title == null
                                || body.getData().form.questions == null
                                || !("PENDENTE".equals(body.getData().form.status) || "CONCLUIDO".equals(body.getData().form.status))) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ReceivedFormDetails>> call, Throwable error) {
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
