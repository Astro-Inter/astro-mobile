package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.ChatMessagePageData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Carrega o histórico humano e marca a conversa como lida ao abri-la. */
public final class HumanChatHistoryRepository {
    public interface ResultCallback {
        void onSuccess(ChatMessagePageData page);
        void onFailure(FailureKind failure);
        void onMarkedRead();
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<?> currentCall;
    private int requestId;

    public HumanChatHistoryRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static HumanChatHistoryRepository createDefault() {
        return new HumanChatHistoryRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void loadConversation(String conversationId, ResultCallback callback) {
        cancel();
        int id = requestId;
        markRead(conversationId, false, id, failure -> {
            if (failure != null) callback.onFailure(failure);
            else {
                callback.onMarkedRead();
                loadPage(conversationId, null, false, id, callback);
            }
        });
    }

    public void loadOlder(String conversationId, String cursor, ResultCallback callback) {
        cancel();
        loadPage(conversationId, cursor, false, requestId, callback);
    }

    private interface ReadCallback { void onComplete(FailureKind failure); }

    private void markRead(String conversationId, boolean refreshed, int id, ReadCallback callback) {
        getToken(refreshed, id, (token, tokenFailure) -> {
            if (id != requestId) return;
            if (tokenFailure != null) {
                callback.onComplete(tokenFailure);
                return;
            }
            Call<Void> call = api.markChatRead("Bearer " + token, conversationId);
            currentCall = call;
            call.enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (id != requestId) return;
                    currentCall = null;
                    if (response.errorBody() != null) response.errorBody().close();
                    if (response.code() == 401 && !refreshed) {
                        markRead(conversationId, true, id, callback);
                    } else if (response.code() == 204) {
                        callback.onComplete(null);
                    } else {
                        callback.onComplete(response.code() == 401 ? FailureKind.SESSION : FailureKind.INTERNAL);
                    }
                }

                @Override public void onFailure(Call<Void> call, Throwable error) {
                    if (id != requestId) return;
                    currentCall = null;
                    callback.onComplete(ApiFailures.transport(error));
                }
            });
        });
    }

    private void loadPage(String conversationId, String cursor, boolean refreshed, int id,
                          ResultCallback callback) {
        getToken(refreshed, id, (token, tokenFailure) -> {
            if (id != requestId) return;
            if (tokenFailure != null) {
                callback.onFailure(tokenFailure);
                return;
            }
            Call<ApiResponse<ChatMessagePageData>> call = api.getChatMessages(
                    "Bearer " + token, conversationId, 30, cursor);
            currentCall = call;
            call.enqueue(new Callback<ApiResponse<ChatMessagePageData>>() {
                @Override public void onResponse(Call<ApiResponse<ChatMessagePageData>> request,
                                                 Response<ApiResponse<ChatMessagePageData>> response) {
                    if (id != requestId) return;
                    currentCall = null;
                    if (response.errorBody() != null) response.errorBody().close();
                    if (response.code() == 401 && !refreshed) {
                        loadPage(conversationId, cursor, true, id, callback);
                        return;
                    }
                    ApiResponse<ChatMessagePageData> body = response.body();
                    if (response.code() != 200 || body == null || !body.isSuccess()
                            || body.getData() == null || !body.getData().isValid(conversationId)) {
                        callback.onFailure(response.code() == 401 ? FailureKind.SESSION : FailureKind.INTERNAL);
                        return;
                    }
                    callback.onSuccess(body.getData());
                }

                @Override public void onFailure(Call<ApiResponse<ChatMessagePageData>> request, Throwable error) {
                    if (id != requestId) return;
                    currentCall = null;
                    callback.onFailure(ApiFailures.transport(error));
                }
            });
        });
    }

    private interface TokenCallback {
        void onResult(String token, FailureKind failure);
    }

    private void getToken(boolean refresh, int id, TokenCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id == requestId) callback.onResult(token, null);
            }

            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id != requestId) return;
                callback.onResult(null, failure == IdTokenProvider.Failure.SESSION ? FailureKind.SESSION
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
