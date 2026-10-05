package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyKeyRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccessKeyRepository {
    public interface ResultCallback {
        void onSuccess();

        void onFailure(FailureKind kind, String message);
    }

    private final AstroApi api;

    public AccessKeyRepository(AstroApi api) {
        this.api = api;
    }

    public Call<Void> verifyKey(String email, String accessKey, ResultCallback callback) {
        // O backend recebe o e-mail e a chave; nenhum token é necessário neste POST.
        Call<Void> call = api.verifyKey(new VerifyKeyRequest(email, accessKey));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (call.isCanceled()) {
                    return;
                }
                // O sucesso contratado é 204, sem corpo de resposta.
                if (response.code() == 204) {
                    callback.onSuccess();
                    return;
                }
                // Apenas 400 e 403 são erros de negócio previstos para a chave.
                if (response.code() != 400 && response.code() != 403) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }

                // A API também retorna 403 vazio; a tela já possui uma mensagem reserva.
                ResponseBody errorBody = response.errorBody();
                if (response.code() == 403
                        && (errorBody == null || errorBody.contentLength() == 0)) {
                    if (errorBody != null) {
                        errorBody.close();
                    }
                    callback.onFailure(FailureKind.BUSINESS, null);
                    return;
                }

                ApiResponse<?> envelope = ApiFailures.read(errorBody);
                String message = ApiFailures.message(envelope);
                if (message == null) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }
                callback.onFailure(FailureKind.BUSINESS, message);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable error) {
                // Timeout também cancela a chamada; o ViewModel descarta só tentativas antigas.
                callback.onFailure(ApiFailures.transport(error), null);
            }
        });
        return call;
    }
}
