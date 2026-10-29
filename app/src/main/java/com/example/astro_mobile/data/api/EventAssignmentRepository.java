package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.EventAssignmentData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import retrofit2.Call;
import retrofit2.Response;

/** Consulta o participante e registra a conclusão com a sessão Firebase. */
public final class EventAssignmentRepository {
    public interface Callback<T> { void onSuccess(T data); void onFailure(FailureKind failure); }
    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<?> currentCall;
    private int requestId;
    public EventAssignmentRepository(AstroApi api, IdTokenProvider tokens) { this.api = api; this.tokens = tokens; }
    public static EventAssignmentRepository createDefault() {
        return new EventAssignmentRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }
    public void load(long eventId, long participantId, Callback<EventAssignmentData> callback) {
        cancel(); request(eventId, participantId, false, false, requestId, callback);
    }
    public void submit(long eventId, long participantId, Callback<EventAssignmentData> callback) {
        cancel(); request(eventId, participantId, true, false, requestId, callback);
    }
    private void request(long eventId, long participantId, boolean submit, boolean refresh,
                         int id, Callback<EventAssignmentData> callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                if (submit) {
                    Call<ApiResponse<Object>> call = api.assignEventConclusion("Bearer " + token, eventId, participantId);
                    currentCall = call;
                    call.enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
                        @Override public void onResponse(@NonNull Call<ApiResponse<Object>> c, @NonNull Response<ApiResponse<Object>> r) {
                            if (id != requestId) return;
                            currentCall = null;
                            if (r.errorBody() != null) r.errorBody().close();
                            if (r.code() == 401) { retryToken(eventId, participantId, true, refresh, id, callback); return; }
                            if (r.code() == 201 && r.body() != null && r.body().isSuccess()) callback.onSuccess(null);
                            else callback.onFailure(FailureKind.INTERNAL);
                        }
                        @Override public void onFailure(@NonNull Call<ApiResponse<Object>> c, @NonNull Throwable error) {
                            if (id == requestId && !c.isCanceled()) { currentCall = null; callback.onFailure(ApiFailures.transport(error)); }
                        }
                    });
                } else {
                    Call<ApiResponse<EventAssignmentData>> call = api.getEventAssignment("Bearer " + token, participantId, eventId);
                    currentCall = call;
                    call.enqueue(new retrofit2.Callback<ApiResponse<EventAssignmentData>>() {
                        @Override public void onResponse(@NonNull Call<ApiResponse<EventAssignmentData>> c, @NonNull Response<ApiResponse<EventAssignmentData>> r) {
                            if (id != requestId) return;
                            currentCall = null;
                            if (r.errorBody() != null) r.errorBody().close();
                            if (r.code() == 401) { retryToken(eventId, participantId, false, refresh, id, callback); return; }
                            ApiResponse<EventAssignmentData> body = r.body();
                            if (!r.isSuccessful() || body == null || !body.isSuccess() || body.getData() == null
                                    || body.getData().participantId == null || body.getData().participantId != participantId) {
                                callback.onFailure(FailureKind.INTERNAL); return;
                            }
                            callback.onSuccess(body.getData());
                        }
                        @Override public void onFailure(@NonNull Call<ApiResponse<EventAssignmentData>> c, @NonNull Throwable error) {
                            if (id == requestId && !c.isCanceled()) { currentCall = null; callback.onFailure(ApiFailures.transport(error)); }
                        }
                    });
                }
            }
            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id != requestId) return;
                callback.onFailure(failure == IdTokenProvider.Failure.SESSION ? FailureKind.SESSION
                        : failure == IdTokenProvider.Failure.CONNECTION ? FailureKind.CONNECTION : FailureKind.INTERNAL);
            }
        });
    }
    private void retryToken(long eventId, long participantId, boolean submit, boolean refreshed,
                            int id, Callback<EventAssignmentData> callback) {
        if (refreshed) callback.onFailure(FailureKind.SESSION);
        else request(eventId, participantId, submit, true, id, callback);
    }
    public void cancel() { requestId++; if (currentCall != null) { currentCall.cancel(); currentCall = null; } }
}
