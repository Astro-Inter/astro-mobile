package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.firebase.IdTokenProvider;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.UserProfileData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class UserProfileRepository {
    public interface ResultCallback {
        void onSuccess(UserProfileData profile);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<UserProfileData>> currentCall;
    private int requestId;

    public UserProfileRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static UserProfileRepository createDefault() {
        return new UserProfileRepository(AstroApiClient.getApi(),
                FirebaseIdTokenProvider.createDefault());
    }

    public void load(ResultCallback callback) {
        // O token vem da sessão Firebase existente, sem outro login ou token salvo.
        cancel();
        loadWithToken(false, requestId, callback);
    }

    private void loadWithToken(boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override
            public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getProfile("Bearer " + token);
                currentCall.enqueue(new Callback<ApiResponse<UserProfileData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserProfileData>> call,
                                           Response<ApiResponse<UserProfileData>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        // Um 401 permite apenas uma renovação e uma nova tentativa.
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(true, id, callback);
                            return;
                        }
                        ApiResponse<UserProfileData> body = response.body();
                        if (response.code() != 200 || body == null
                                || !body.isSuccess() || body.getData() == null) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserProfileData>> call, Throwable error) {
                        if (id != requestId) return;
                        currentCall = null;
                        // Neste perfil, rede, timeout e demais falhas usam o erro genérico.
                        callback.onFailure(FailureKind.INTERNAL);
                    }
                });
            }

            @Override
            public void onFailure(IdTokenProvider.Failure failure) {
                if (id == requestId) {
                    callback.onFailure(failure == IdTokenProvider.Failure.SESSION
                            ? FailureKind.SESSION : FailureKind.INTERNAL);
                }
            }
        });
    }

    public void cancel() {
        // Descarta respostas tardias e cancela o HTTP ao sair da tela.
        ++requestId;
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
    }
}
