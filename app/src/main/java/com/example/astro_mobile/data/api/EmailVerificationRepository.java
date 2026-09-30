package com.example.astro_mobile.data.api;

import androidx.annotation.NonNull;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyEmailData;
import com.example.astro_mobile.data.api.dto.VerifyEmailRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailVerificationRepository {
    public interface ResultCallback {
        void onSuccess(VerifyEmailData data);

        void onFailure(FailureKind kind, String message);
    }

    private static final Type RESPONSE_TYPE =
            new TypeToken<ApiResponse<VerifyEmailData>>() { }.getType();

    private final AstroApi api;
    private final Gson gson = new Gson();

    public EmailVerificationRepository(AstroApi api) {
        this.api = api;
    }

    public Call<ApiResponse<VerifyEmailData>> verifyEmail(String email, ResultCallback callback) {
        // Envia o e-mail e devolve ao ViewModel um resultado ou uma falha classificada.
        Call<ApiResponse<VerifyEmailData>> call = api.verifyEmail(new VerifyEmailRequest(email));
        call.enqueue(new Callback<ApiResponse<VerifyEmailData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<VerifyEmailData>> call,
                                   @NonNull Response<ApiResponse<VerifyEmailData>> response) {
                // Respostas HTTP chegaram à API; 5xx e corpos inválidos são falhas internas.
                if (response.code() >= 500) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }

                ApiResponse<VerifyEmailData> envelope = response.isSuccessful()
                        ? response.body() : parseError(response.errorBody());
                if (envelope == null) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }
                if (response.isSuccessful() && envelope.isSuccess() && envelope.getData() != null) {
                    callback.onSuccess(envelope.getData());
                    return;
                }
                if ("Erro interno do servidor".equals(envelope.getMessage())) {
                    callback.onFailure(FailureKind.INTERNAL, null);
                    return;
                }
                String message = firstErrorOrMessage(envelope);
                callback.onFailure(message == null ? FailureKind.INTERNAL : FailureKind.BUSINESS,
                        message);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<VerifyEmailData>> call,
                                  @NonNull Throwable error) {
                // Sem resposta HTTP, só falhas claras de acesso à rede indicam sem conexão.
                if (!call.isCanceled()) {
                    callback.onFailure(classifyTransportFailure(error), null);
                }
            }
        });
        return call;
    }

    private static FailureKind classifyTransportFailure(Throwable error) {
        // Timeout pode ser o servidor acordando; não significa que o aparelho esteja offline.
        if (error instanceof UnknownHostException
                || error instanceof ConnectException
                || error instanceof NoRouteToHostException) {
            return FailureKind.CONNECTION;
        }
        return FailureKind.INTERNAL;
    }

    private ApiResponse<VerifyEmailData> parseError(ResponseBody body) {
        // Lê a mesma estrutura de resposta também quando o HTTP indica erro.
        if (body == null) {
            return null;
        }
        try (ResponseBody errorBody = body) {
            return gson.fromJson(errorBody.charStream(), RESPONSE_TYPE);
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static String firstErrorOrMessage(ApiResponse<?> envelope) {
        // Prioriza a primeira mensagem específica e usa a mensagem geral como reserva.
        List<String> errors = envelope.getErrors();
        if (errors != null && !errors.isEmpty() && errors.get(0) != null
                && !errors.get(0).trim().isEmpty()) {
            return errors.get(0);
        }
        String message = envelope.getMessage();
        return message == null || message.trim().isEmpty() ? null : message;
    }
}
