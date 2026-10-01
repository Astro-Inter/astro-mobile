package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyKeyRequest;

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
                boolean hasBody = response.errorBody() != null
                        && response.errorBody().contentLength() != 0;
                ApiResponse<?> envelope = ApiFailures.read(response.errorBody());
                String message = ApiFailures.message(envelope);
                if ((response.code() != 400 && response.code() != 403)
                        || (envelope == null && (hasBody || response.code() != 403))
                        || (hasBody && message == null)
                        || (envelope != null
                        && "Erro interno do servidor".equals(envelope.getMessage()))) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }
                // 400 e 403 ficam no campo; a View fornece o texto reserva se não houver mensagem.
                callback.onFailure(FailureKind.BUSINESS, message);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable error) {
                if (!call.isCanceled()) {
                    callback.onFailure(ApiFailures.transport(error), null);
                }
            }
        });
        return call;
    }
}
