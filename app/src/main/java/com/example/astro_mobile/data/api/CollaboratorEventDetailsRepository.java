package com.example.astro_mobile.data.api;

import android.content.ContentResolver;
import android.util.Log;
import com.example.astro_mobile.BuildConfig;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.CollaboratorEventDetailsData;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import java.util.List;
import java.util.function.Function;

import okhttp3.MultipartBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class CollaboratorEventDetailsRepository {
    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onFailure(FailureKind failure, String message);
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<?> currentCall;
    private int requestId;

    public CollaboratorEventDetailsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static CollaboratorEventDetailsRepository createDefault() {
        return new CollaboratorEventDetailsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void load(long eventId, ResultCallback<CollaboratorEventDetailsData> callback) {
        cancel();
        execute(token -> api.getCollaboratorEvent("Bearer " + token, eventId),
                200, false, requestId, new ResultCallback<CollaboratorEventDetailsData>() {
                    @Override public void onSuccess(CollaboratorEventDetailsData data) {
                        if (data == null || !data.isValid() || data.getEventId() != eventId) {
                            callback.onFailure(FailureKind.INTERNAL, null);
                        } else callback.onSuccess(data);
                    }
                    @Override public void onFailure(FailureKind failure, String message) {
                        callback.onFailure(failure, message);
                    }
                });
    }

    public void complete(long eventId, List<EventEvidenceFile> files, ContentResolver resolver,
                         ResultCallback<Object> callback) {
        cancel();
        RequestBody body;
        if (files.isEmpty()) {
            // Um multipart sem arquivos continua válido: só contém o delimitador final.
            body = RequestBody.create(MediaType.get("multipart/form-data; boundary=astro-evidence"),
                    "--astro-evidence--\r\n");
        } else {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (EventEvidenceFile file : files) builder.addPart(file.toPart(resolver));
            body = builder.build();
        }
        execute(token -> api.completeCollaboratorEvent("Bearer " + token, eventId, body),
                201, false, requestId, callback);
    }

    private <T> void execute(Function<String, Call<ApiResponse<T>>> request, int successCode,
                             boolean refresh, int id, ResultCallback<T> callback) {
        // Consulta e envio compartilham a renovação única de token e o descarte ao sair.
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                Call<ApiResponse<T>> call = request.apply(token);
                currentCall = call;
                call.enqueue(new Callback<ApiResponse<T>>() {
                    @Override public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (BuildConfig.DEBUG) Log.d("AstroEventApi", call.request().method() + " "
                                + call.request().url().encodedPath() + " HTTP " + response.code());
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION, null);
                            else execute(request, successCode, true, id, callback);
                            return;
                        }
                        ApiResponse<T> body = response.body();
                        if (response.code() == successCode && body != null && body.isSuccess()) {
                            callback.onSuccess(body.getData());
                            return;
                        }
                        if (response.code() == 400 || response.code() == 413) {
                            callback.onFailure(FailureKind.BUSINESS, ApiFailures.message(ApiFailures.read(response.errorBody())));
                            return;
                        }
                        callback.onFailure(FailureKind.INTERNAL, null);
                    }
                    @Override public void onFailure(Call<ApiResponse<T>> call, Throwable error) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (BuildConfig.DEBUG) Log.w("AstroEventApi", "Falha em "
                                + call.request().url().encodedPath(), error);
                        callback.onFailure(ApiFailures.transport(error), null);
                    }
                });
            }
            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id == requestId) callback.onFailure(FailureKind.valueOf(failure.name()), null);
            }
        });
    }

    public void cancel() {
        requestId++;
        if (currentCall != null) currentCall.cancel();
        currentCall = null;
    }
}
