package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyEmailData;
import com.example.astro_mobile.data.api.dto.VerifyEmailRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailVerificationRepository {
    public interface ResultCallback {
        void onSuccess(VerifyEmailData data);

        void onFailure(FailureKind kind, String message);
    }

    private final AstroApi api;

    public EmailVerificationRepository(AstroApi api) {
        this.api = api;
    }

    public Call<ApiResponse<VerifyEmailData>> verifyEmail(String email, ResultCallback callback) {
        // Envia o e-mail e devolve ao ViewModel um resultado ou uma falha classificada.
        Call<ApiResponse<VerifyEmailData>> call = api.verifyEmail(new VerifyEmailRequest(email));
        call.enqueue(new Callback<ApiResponse<VerifyEmailData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<VerifyEmailData>> call,
                                   @NonNull Response<ApiResponse<VerifyEmailData>> response) {
                // Respostas HTTP chegaram à API; 5xx e corpos inválidos são falhas internas.
                if (call.isCanceled()) {
                    return;
                }
                if (response.code() >= 500) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }

                ApiResponse<?> envelope = response.isSuccessful()
                        ? response.body() : ApiFailures.read(response.errorBody());
                if (envelope == null) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }
                if (response.isSuccessful() && envelope.isSuccess()) {
                    if (envelope.getData() == null) {
                        callback.onFailure(FailureKind.INTERNAL, null);
                        return;
                    }
                    callback.onSuccess(response.body().getData());
                    return;
                }
                String message = ApiFailures.message(envelope);
                callback.onFailure(message == null ? FailureKind.INTERNAL : FailureKind.BUSINESS,
                        message);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<VerifyEmailData>> call,
                                  @NonNull Throwable error) {
                // Timeout não é falta de internet. O ViewModel ignora o cancelamento manual.
                callback.onFailure(ApiFailures.transport(error), null);
            }
        });
        return call;
    }
}
