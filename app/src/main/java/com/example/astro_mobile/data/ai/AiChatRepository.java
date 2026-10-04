package com.example.astro_mobile.data.ai;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Envia mensagens com o ID token Firebase. Não usa credenciais nem /auth/login. */
public final class AiChatRepository {
    public interface ResultCallback {
        void onSuccess(ChatResponse response);
        void onFailure(ChatFailure failure);
    }

    public static final class Operation {
        private final AtomicBoolean canceled = new AtomicBoolean();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile Call<ChatResponse> call;

        public void cancel() {
            canceled.set(true);
            Call<ChatResponse> pending = call;
            if (pending != null) pending.cancel();
        }

        private boolean canContinue() { return !canceled.get() && !finished.get(); }
        private boolean complete() { return !canceled.get() && finished.compareAndSet(false, true); }

        private void setCall(Call<ChatResponse> call) {
            this.call = call;
            if (canceled.get()) call.cancel();
        }
    }

    private final AiChatApi api;
    private final IdTokenProvider tokens;

    public AiChatRepository(AiChatApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static AiChatRepository createDefault() {
        return new AiChatRepository(AiApiClient.create(AiChatApi.class),
                FirebaseIdTokenProvider.createDefault());
    }

    public Operation sendMessage(String message, String sessionId, ResultCallback callback) {
        Operation operation = new Operation();
        String normalized = message == null ? "" : message.trim();
        if (normalized.isEmpty()
                || normalized.codePointCount(0, normalized.length()) > ChatRequest.MAX_MESSAGE_LENGTH) {
            fail(operation, callback, ChatFailure.INVALID_MESSAGE);
        } else {
            authenticate(operation, new ChatRequest(normalized, sessionId), false, callback);
        }
        return operation;
    }

    private void authenticate(Operation operation, ChatRequest request, boolean refreshed,
                              ResultCallback callback) {
        tokens.getToken(refreshed, new IdTokenProvider.Callback() {
            @Override
            public void onToken(String token) {
                if (!operation.canContinue()) return;
                if (token == null || token.isEmpty()) {
                    fail(operation, callback, ChatFailure.SESSION);
                    return;
                }
                send(operation, request, token, refreshed, callback);
            }

            @Override
            public void onFailure(IdTokenProvider.Failure failure) {
                fail(operation, callback, failure == IdTokenProvider.Failure.SESSION
                        ? ChatFailure.SESSION : failure == IdTokenProvider.Failure.CONNECTION
                        ? ChatFailure.CONNECTION : ChatFailure.SERVER);
            }
        });
    }

    private void send(Operation operation, ChatRequest request, String token, boolean refreshed,
                      ResultCallback callback) {
        Call<ChatResponse> call = api.sendMessage("Bearer " + token, true, request);
        operation.setCall(call);
        if (!operation.canContinue()) return;
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call,
                                   @NonNull Response<ChatResponse> response) {
                if (!operation.canContinue()) {
                    closeErrorBody(response);
                    return;
                }
                int code = response.code();
                // Só repete após rejeição de autenticação, nunca após timeout de um POST.
                if (code == 401 && !refreshed) {
                    closeErrorBody(response);
                    authenticate(operation, request, true, callback);
                    return;
                }
                if (response.isSuccessful()) {
                    ChatResponse body = response.body();
                    if (body != null && body.isValid()) {
                        if (operation.complete()) callback.onSuccess(body);
                    } else {
                        fail(operation, callback, ChatFailure.INVALID_RESPONSE);
                    }
                    return;
                }
                closeErrorBody(response);
                ChatFailure failure = code == 401 ? ChatFailure.SESSION
                        : code == 403 ? ChatFailure.FORBIDDEN
                        : code == 429 ? ChatFailure.RATE_LIMIT
                        : code == 400 || code == 422 ? ChatFailure.INVALID_MESSAGE
                        : code == 408 || code == 504 ? ChatFailure.TIMEOUT : ChatFailure.SERVER;
                fail(operation, callback, failure);
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable error) {
                if (call.isCanceled()) return;
                fail(operation, callback, error instanceof InterruptedIOException
                        ? ChatFailure.TIMEOUT : error instanceof IOException
                        ? ChatFailure.CONNECTION : ChatFailure.INVALID_RESPONSE);
            }
        });
    }

    private static void closeErrorBody(Response<?> response) {
        if (response.errorBody() != null) response.errorBody().close();
    }

    private static void fail(Operation operation, ResultCallback callback, ChatFailure failure) {
        if (operation.complete()) callback.onFailure(failure);
    }
}
