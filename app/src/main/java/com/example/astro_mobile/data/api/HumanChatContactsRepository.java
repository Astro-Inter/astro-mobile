package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.ChatContactData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class HumanChatContactsRepository {
    public interface ResultCallback {
        void onSuccess(List<ChatContactData> contacts);
        void onFailure(FailureKind failure);
    }
    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<?> currentCall;
    private int requestId;

    public HumanChatContactsRepository(AstroApi api, IdTokenProvider tokens) { this.api = api; this.tokens = tokens; }
    public static HumanChatContactsRepository createDefault() {
        return new HumanChatContactsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }
    public void load(ResultCallback callback) { cancel(); load(false, requestId, callback); }
    private void load(boolean refresh, int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                Call<ApiResponse<List<ChatContactData>>> call = api.getChatContacts("Bearer " + token);
                currentCall = call;
                call.enqueue(new Callback<ApiResponse<List<ChatContactData>>>() {
                    @Override public void onResponse(Call<ApiResponse<List<ChatContactData>>> call,
                                                     Response<ApiResponse<List<ChatContactData>>> response) {
                        if (response.errorBody() != null) response.errorBody().close();
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.code() == 401 && !refresh) { load(true, id, callback); return; }
                        ApiResponse<List<ChatContactData>> body = response.body();
                        if (response.code() != 200 || body == null || !body.isSuccess() || body.getData() == null
                                || body.getData().stream().anyMatch(item -> item == null || !item.isValid())) {
                            callback.onFailure(response.code() == 401 ? FailureKind.SESSION : FailureKind.INTERNAL);
                        } else callback.onSuccess(body.getData());
                    }
                    @Override public void onFailure(Call<ApiResponse<List<ChatContactData>>> call, Throwable error) {
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
    public void cancel() {
        ++requestId;
        if (currentCall != null) { currentCall.cancel(); currentCall = null; }
    }
}
