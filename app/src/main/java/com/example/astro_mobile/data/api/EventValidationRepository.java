package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.EventRejectionRequest;
import com.example.astro_mobile.data.api.dto.EventValidationData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import java.util.function.Function;
import retrofit2.Call;
import retrofit2.Response;

/** Consulta e valida conclusões com a sessão autenticada. */
public final class EventValidationRepository {
    public interface Callback<T> { void onSuccess(T data); void onFailure(FailureKind failure); }
    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<?> currentCall;
    private int requestId;
    public EventValidationRepository(AstroApi api, IdTokenProvider tokens) { this.api = api; this.tokens = tokens; }
    public static EventValidationRepository createDefault() {
        return new EventValidationRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }
    public void load(long eventId, long participantId, Callback<EventValidationData> callback) {
        cancel(); request(token -> api.getEventValidation(token, participantId, eventId), false, requestId, callback);
    }
    public void decide(long eventId, long participantId, String reason, Callback<Object> callback) {
        cancel();
        request(token -> reason == null ? api.approveEventConclusion(token, eventId, participantId)
                : api.rejectEventConclusion(token, eventId, participantId, new EventRejectionRequest(reason)), false, requestId, callback);
    }
    private <T> void request(Function<String, Call<ApiResponse<T>>> operation, boolean refreshed,
                             int id, Callback<T> callback) {
        tokens.getToken(refreshed, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                Call<ApiResponse<T>> call = operation.apply("Bearer " + token);
                currentCall = call;
                call.enqueue(new retrofit2.Callback<ApiResponse<T>>() {
                    @Override public void onResponse(@NonNull Call<ApiResponse<T>> c, @NonNull Response<ApiResponse<T>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.errorBody() != null) response.errorBody().close();
                        if (response.code() == 401) {
                            if (refreshed) callback.onFailure(FailureKind.SESSION);
                            else request(operation, true, id, callback);
                            return;
                        }
                        ApiResponse<T> body = response.body();
                        if (response.code() != 200 || body == null || !body.isSuccess() || body.getData() == null) {
                            callback.onFailure(FailureKind.INTERNAL); return;
                        }
                        callback.onSuccess(body.getData());
                    }
                    @Override public void onFailure(@NonNull Call<ApiResponse<T>> c, @NonNull Throwable error) {
                        if (id == requestId && !c.isCanceled()) { currentCall = null; callback.onFailure(ApiFailures.transport(error)); }
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
    public void cancel() { requestId++; if (currentCall != null) { currentCall.cancel(); currentCall = null; } }
}
