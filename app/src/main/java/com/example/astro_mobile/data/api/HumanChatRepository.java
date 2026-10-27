package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ChatConversationData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Consulta as conversas da conta autenticada, mantendo os detalhes do HTTP fora da tela. */
public final class HumanChatRepository {
    public interface ResultCallback {
        void onSuccess(List<ChatConversationData> conversations);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<List<ChatConversationData>> currentCall;
    private int requestId;

    public HumanChatRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static HumanChatRepository createDefault() {
        return new HumanChatRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void load(ResultCallback callback) {
        cancel();
        loadWithToken(false, requestId, callback);
    }

    private void loadWithToken(boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getChats("Bearer " + token);
                currentCall.enqueue(new Callback<List<ChatConversationData>>() {
                    @Override public void onResponse(Call<List<ChatConversationData>> call,
                                                     Response<List<ChatConversationData>> response) {
                        if (response.errorBody() != null) response.errorBody().close();
                        if (id != requestId) return;
                        currentCall = null;
                        // Uma segunda rejeição do token encerra a tentativa de autenticação.
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(true, id, callback);
                            return;
                        }
                        // Este endpoint retorna a lista diretamente, sem o envelope ApiResponse.
                        List<ChatConversationData> body = response.body();
                        if (response.code() != 200 || body == null || body.stream()
                                .anyMatch(item -> item == null || !item.isValid())) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body);
                    }

                    @Override public void onFailure(Call<List<ChatConversationData>> call, Throwable error) {
                        if (id != requestId) return;
                        currentCall = null;
                        callback.onFailure(ApiFailures.transport(error));
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
