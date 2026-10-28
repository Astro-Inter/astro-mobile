package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.ManagerEventData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class ManagerEventsRepository {
    public interface ResultCallback {
        void onSuccess(List<ManagerEventData> events);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<List<ManagerEventData>>> currentCall;
    private int requestId;

    public ManagerEventsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static ManagerEventsRepository createDefault() {
        return new ManagerEventsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    // Obtém o token e consulta os eventos do gestor autenticado.
    public void load(ResultCallback callback) {
        cancel();
        loadWithToken(false, requestId, callback);
    }

    private void loadWithToken(boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getManagerEvents("Bearer " + token);
                currentCall.enqueue(new Callback<ApiResponse<List<ManagerEventData>>>() {
                    @Override public void onResponse(Call<ApiResponse<List<ManagerEventData>>> call,
                                                     Response<ApiResponse<List<ManagerEventData>>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        // Tenta renovar a sessão uma vez quando o token expira.
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(true, id, callback);
                            return;
                        }
                        ApiResponse<List<ManagerEventData>> body = response.body();
                        if (response.code() != 200 || body == null || !body.isSuccess() || body.getData() == null) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override public void onFailure(Call<ApiResponse<List<ManagerEventData>>> call, Throwable error) {
                        if (id != requestId) return;
                        currentCall = null;
                        callback.onFailure(ApiFailures.transport(error));
                    }
                });
            }

            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id != requestId) return;
                callback.onFailure(failure == IdTokenProvider.Failure.SESSION ? FailureKind.SESSION
                        : failure == IdTokenProvider.Failure.CONNECTION ? FailureKind.CONNECTION : FailureKind.INTERNAL);
            }
        });
    }

    // Cancela a consulta ao sair da tela e ignora respostas antigas.
    public void cancel() {
        ++requestId;
        if (currentCall != null) currentCall.cancel();
        currentCall = null;
    }
}
