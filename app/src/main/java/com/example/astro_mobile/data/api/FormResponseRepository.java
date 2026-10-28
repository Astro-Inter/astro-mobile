package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.FormImageUrl;
import com.example.astro_mobile.data.api.dto.FormResponseRequest;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Envia e consulta respostas de formulários recebidos pelo usuário autenticado. */
public final class FormResponseRepository {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType JPEG = MediaType.parse("image/jpeg");

    public interface ResultCallback {
        void onSuccess(ReceivedFormDetails data);
        void onFailure(FailureKind failure, String message, boolean uncertain);
        default void onNotSubmitted() { }
    }

    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<ReceivedFormDetails>> currentCall;
    private int requestId;

    public FormResponseRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }

    public static FormResponseRepository createDefault() {
        return new FormResponseRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }

    public void submit(String formId, FormResponseRequest request, ResultCallback callback) {
        submit(formId, request, new ArrayList<>(), callback);
    }

    public void submit(String formId, FormResponseRequest request, List<File> photos,
                       ResultCallback callback) {
        cancel();
        execute(formId, Mode.SUBMIT, false, request, photos, requestId, callback);
    }

    public void load(String formId, boolean submitted, ResultCallback callback) {
        cancel();
        execute(formId, submitted ? Mode.READ_RESPONSES : Mode.READ_FORM, false,
                null, null, requestId, callback);
    }

    /** Confirma um POST sem resposta antes de permitir que o usuário envie novamente. */
    public void confirmSubmission(String formId, ResultCallback callback) {
        cancel();
        execute(formId, Mode.CONFIRM_SUBMISSION, false, null, null, requestId, callback);
    }

    public interface ImageUrlCallback {
        void onSuccess(String url);
        void onFailure(FailureKind failure);
    }

    public void getImageUrl(String formId, String imageId, ImageUrlCallback callback) {
        getImageUrl(formId, imageId, false, callback);
    }

    private void getImageUrl(String formId, String imageId, boolean refreshToken,
                             ImageUrlCallback callback) {
        tokens.getToken(refreshToken, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                api.getFormImageUrl("Bearer " + token, formId, imageId).enqueue(
                        new Callback<ApiResponse<FormImageUrl>>() {
                            @Override public void onResponse(Call<ApiResponse<FormImageUrl>> call,
                                    Response<ApiResponse<FormImageUrl>> response) {
                                if (response.code() == 401) {
                                    if (refreshToken) callback.onFailure(FailureKind.SESSION);
                                    else getImageUrl(formId, imageId, true, callback);
                                    return;
                                }
                                ApiResponse<FormImageUrl> body = response.body();
                                FormImageUrl data = body == null ? null : body.getData();
                                if (response.code() == 200 && body != null && body.isSuccess()
                                        && data != null && data.url != null && !data.url.isEmpty()) {
                                    callback.onSuccess(data.url);
                                } else {
                                    callback.onFailure(FailureKind.INTERNAL);
                                }
                            }
                            @Override public void onFailure(Call<ApiResponse<FormImageUrl>> call,
                                    Throwable error) {
                                callback.onFailure(ApiFailures.transport(error));
                            }
                        });
            }
            @Override public void onFailure(IdTokenProvider.Failure failure) {
                callback.onFailure(FailureKind.valueOf(failure.name()));
            }
        });
    }

    private void execute(String formId, Mode mode, boolean refreshToken, FormResponseRequest request,
                         List<File> photos, int id, ResultCallback callback) {
        tokens.getToken(refreshToken, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                Call<ApiResponse<ReceivedFormDetails>> call = mode == Mode.SUBMIT
                        ? photos == null || photos.isEmpty()
                            ? api.submitFormResponse("Bearer " + token, formId, request)
                            : api.submitFormResponseWithFiles("Bearer " + token, formId,
                                    RequestBody.create(JSON, new Gson().toJson(request)), photoParts(photos))
                        : mode == Mode.READ_FORM
                        ? api.getReceivedForm("Bearer " + token, formId)
                        : api.getFormResponses("Bearer " + token, formId);
                currentCall = call;
                call.enqueue(new Callback<ApiResponse<ReceivedFormDetails>>() {
                    @Override public void onResponse(Call<ApiResponse<ReceivedFormDetails>> call,
                                                     Response<ApiResponse<ReceivedFormDetails>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        if (response.code() == 401) {
                            if (refreshToken) callback.onFailure(FailureKind.SESSION, null,
                                    mode == Mode.CONFIRM_SUBMISSION);
                            else execute(formId, mode, true, request, photos, id, callback);
                            return;
                        }
                        if (response.code() == 409) {
                            if (mode == Mode.CONFIRM_SUBMISSION) callback.onNotSubmitted();
                            else callback.onFailure(FailureKind.BUSINESS,
                                    ApiFailures.message(ApiFailures.read(response.errorBody())), mode == Mode.SUBMIT);
                            return;
                        }
                        if (response.code() == 400 || response.code() == 403 || response.code() == 404) {
                            callback.onFailure(FailureKind.BUSINESS,
                                    ApiFailures.message(ApiFailures.read(response.errorBody())), false);
                            return;
                        }
                        ApiResponse<ReceivedFormDetails> body = response.body();
                        ReceivedFormDetails data = body == null ? null : body.getData();
                        String status = data == null || data.form == null ? null : data.form.status;
                        boolean valid = response.code() == 200 && body != null && body.isSuccess()
                                && data != null && data.form != null && formId.equals(data.form.id)
                                && data.form.questions != null;
                        if (!valid || mode == Mode.SUBMIT && !"CONCLUIDO".equals(status)
                                || mode == Mode.READ_FORM && !"PENDENTE".equals(status)
                                    && !"CONCLUIDO".equals(status)
                                || (mode == Mode.READ_RESPONSES || mode == Mode.CONFIRM_SUBMISSION)
                                    && !"CONCLUIDO".equals(status)) {
                            boolean uncertain = mode == Mode.SUBMIT && (response.code() >= 500 || response.code() == 200)
                                    || mode == Mode.CONFIRM_SUBMISSION;
                            callback.onFailure(FailureKind.INTERNAL, null, uncertain);
                            return;
                        }
                        callback.onSuccess(data);
                    }

                    @Override public void onFailure(Call<ApiResponse<ReceivedFormDetails>> call, Throwable error) {
                        if (id != requestId) return;
                        currentCall = null;
                        callback.onFailure(ApiFailures.transport(error), null,
                                mode == Mode.SUBMIT || mode == Mode.CONFIRM_SUBMISSION);
                    }
                });
            }

            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id == requestId) callback.onFailure(FailureKind.valueOf(failure.name()), null,
                        mode == Mode.CONFIRM_SUBMISSION);
            }
        });
    }

    private List<MultipartBody.Part> photoParts(List<File> photos) {
        List<MultipartBody.Part> parts = new ArrayList<>();
        for (File photo : photos) {
            RequestBody body = RequestBody.create(JPEG, photo);
            parts.add(MultipartBody.Part.createFormData("files", photo.getName(), body));
        }
        return parts;
    }

    public void cancel() {
        requestId++;
        if (currentCall != null) currentCall.cancel();
        currentCall = null;
    }

    private enum Mode { SUBMIT, READ_FORM, READ_RESPONSES, CONFIRM_SUBMISSION }
}
