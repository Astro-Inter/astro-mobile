package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.EventParticipantData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Consulta participantes e renova o token Firebase uma vez após 401. */
public final class EventParticipantsRepository {
    public interface ResultCallback {
        void onSuccess(List<EventParticipantData> collaborators);
        void onFailure(FailureKind failure);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<List<EventParticipantData>>> currentCall;
    private int requestId;

    public EventParticipantsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static EventParticipantsRepository createDefault() {
        return new EventParticipantsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void load(long eventId, ResultCallback callback) {
        cancel();
        loadWithToken(eventId, false, requestId, callback);
    }

    private void loadWithToken(long eventId, boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getEventParticipants("Bearer " + token, eventId);
                currentCall.enqueue(new retrofit2.Callback<ApiResponse<List<EventParticipantData>>>() {
                    @Override public void onResponse(
                            @NonNull Call<ApiResponse<List<EventParticipantData>>> call,
                            @NonNull Response<ApiResponse<List<EventParticipantData>>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.errorBody() != null) response.errorBody().close();
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadWithToken(eventId, true, id, callback);
                            return;
                        }
                        ApiResponse<List<EventParticipantData>> body = response.body();
                        if (!response.isSuccessful() || body == null || !body.isSuccess()
                                || body.getData() == null || body.getData().stream()
                                .anyMatch(item -> item == null || !item.isValid())) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        callback.onSuccess(body.getData());
                    }

                    @Override public void onFailure(
                            @NonNull Call<ApiResponse<List<EventParticipantData>>> call,
                            @NonNull Throwable error) {
                        if (id == requestId && !call.isCanceled()) {
                            currentCall = null;
                            callback.onFailure(ApiFailures.transport(error));
                        }
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
