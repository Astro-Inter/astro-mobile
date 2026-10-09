package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ActivateRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivationRepository {
    public interface ResultCallback {
        void onSuccess();

        void onFailure(FailureKind kind);
    }

    private final AstroApi api;

    public ActivationRepository(AstroApi api) {
        this.api = api;
    }

    public Call<Void> activate(String email, String firebaseUid, ResultCallback callback) {
        // Envia somente e-mail e UID; este endpoint não exige token.
        Call<Void> call = api.activate(new ActivateRequest(email, firebaseUid));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (call.isCanceled()) {
                    return;
                }
                // Repetir uma ativação já concluída também retorna 204.
                if (response.code() == 204) {
                    callback.onSuccess();
                    return;
                }
                ResponseBody errorBody = response.errorBody();
                if (errorBody != null) {
                    errorBody.close();
                }
                // 404 e demais falhas HTTP usam o erro genérico, conforme combinado.
                callback.onFailure(FailureKind.INTERNAL);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable error) {
                callback.onFailure(ApiFailures.transport(error));
            }
        });
        return call;
    }
}
