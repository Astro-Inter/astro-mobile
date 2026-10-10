package com.example.astro_mobile.data.ai;

import androidx.annotation.NonNull;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import retrofit2.Call;
import retrofit2.Response;

/** Requisições autenticadas e canceláveis; repete uma vez apenas após 401. */
public final class AiSessionsRepository {
    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(ChatFailure failure);
    }
    private interface Request<T> { Call<T> create(String authorization); }

    public static final class Operation {
        private final AtomicBoolean canceled = new AtomicBoolean();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile Call<?> call;
        public void cancel() { canceled.set(true); if (call != null) call.cancel(); }
        private boolean active() { return !canceled.get() && !finished.get(); }
        private boolean complete() { return !canceled.get() && finished.compareAndSet(false, true); }
        private void attach(Call<?> call) { this.call = call; if (canceled.get()) call.cancel(); }
    }

    private final AiChatApi api;
    private final IdTokenProvider tokens;
    public AiSessionsRepository(AiChatApi api, IdTokenProvider tokens) { this.api = api; this.tokens = tokens; }
    public static AiSessionsRepository createDefault() {
        return new AiSessionsRepository(AiApiClient.create(AiChatApi.class), FirebaseIdTokenProvider.createDefault());
    }

    public Operation list(String cursor, Callback<SessionsPage> callback) {
        return request(token -> api.listSessions(token, 20, cursor), SessionsPage::isValid, true, callback);
    }
    public Operation history(String id, Callback<SessionHistory> callback) {
        return request(token -> api.sessionHistory(token, id), result -> result.isValid() && id.equals(result.sessionId), false, callback);
    }
    public Operation resume(String id, Callback<SessionResult> callback) {
        return request(token -> api.resumeSession(token, id), result -> result.isValid()
                && id.equals(result.sessionId) && "ativa".equals(result.status), false, callback);
    }

    private <T> Operation request(Request<T> request, Predicate<T> valid, boolean listing, Callback<T> callback) {
        Operation operation = new Operation();
        authenticate(operation, request, valid, listing, false, callback);
        return operation;
    }

    private <T> void authenticate(Operation operation, Request<T> request, Predicate<T> valid,
                                  boolean listing, boolean refreshed, Callback<T> callback) {
        tokens.getToken(refreshed, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (!operation.active()) return;
                if (token == null || token.isEmpty()) { fail(operation, callback, ChatFailure.SESSION); return; }
                Call<T> call = request.create("Bearer " + token);
                operation.attach(call);
                if (!operation.active()) return;
                call.enqueue(new retrofit2.Callback<T>() {
                    @Override public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                        if (response.errorBody() != null) response.errorBody().close();
                        if (!operation.active()) return;
                        if (response.code() == 401 && !refreshed) {
                            authenticate(operation, request, valid, listing, true, callback);
                        } else if (response.isSuccessful()) {
                            T body = response.body();
                            if (body != null && valid.test(body)) {
                                if (operation.complete()) callback.onSuccess(body);
                            } else fail(operation, callback, ChatFailure.INVALID_RESPONSE);
                        } else {
                            int code = response.code();
                            fail(operation, callback, code == 401 ? ChatFailure.SESSION : code == 403 ? ChatFailure.FORBIDDEN
                                    : code == 404 ? ChatFailure.NOT_FOUND : code == 409 ? ChatFailure.CONFLICT
                                    : code == 400 && listing ? ChatFailure.INVALID_CURSOR
                                    : code == 422 ? ChatFailure.INVALID_MESSAGE : code == 429 ? ChatFailure.RATE_LIMIT
                                    : code == 408 || code == 504 ? ChatFailure.TIMEOUT : ChatFailure.SERVER);
                        }
                    }
                    @Override public void onFailure(@NonNull Call<T> call, @NonNull Throwable error) {
                        if (!call.isCanceled()) fail(operation, callback, error instanceof InterruptedIOException
                                ? ChatFailure.TIMEOUT : error instanceof IOException ? ChatFailure.CONNECTION : ChatFailure.INVALID_RESPONSE);
                    }
                });
            }
            @Override public void onFailure(IdTokenProvider.Failure failure) {
                fail(operation, callback, failure == IdTokenProvider.Failure.SESSION ? ChatFailure.SESSION
                        : failure == IdTokenProvider.Failure.CONNECTION ? ChatFailure.CONNECTION : ChatFailure.SERVER);
            }
        });
    }
    private static <T> void fail(Operation operation, Callback<T> callback, ChatFailure failure) {
        if (operation.complete()) callback.onFailure(failure);
    }
}
