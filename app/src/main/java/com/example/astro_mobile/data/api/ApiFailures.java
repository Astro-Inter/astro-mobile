package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.google.gson.Gson;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.ResponseBody;

// Compartilha a leitura de erros entre as consultas de e-mail e chave.
public final class ApiFailures {
    private static final Gson GSON = new Gson();

    private ApiFailures() {
    }

    public static ApiResponse<?> read(ResponseBody body) {
        // Um corpo ausente ou fora do padrão não vira uma mensagem de negócio.
        if (body == null) {
            return null;
        }
        try (ResponseBody errorBody = body) {
            return GSON.fromJson(errorBody.charStream(), ApiResponse.class);
        } catch (RuntimeException error) {
            return null;
        }
    }

    public static String message(ApiResponse<?> envelope) {
        if (envelope == null) {
            return null;
        }
        // Prioriza o primeiro erro específico, depois a mensagem geral.
        List<String> errors = envelope.getErrors();
        if (errors != null) {
            for (String error : errors) {
                if (error != null && !error.trim().isEmpty()) {
                    return error;
                }
            }
        }
        String message = envelope.getMessage();
        return message == null || message.trim().isEmpty() ? null : message;
    }

    public static FailureKind transport(Throwable error) {
        // Timeout do servidor, inclusive do Render, não indica falta de internet.
        if (error instanceof UnknownHostException
                || error instanceof ConnectException
                || error instanceof NoRouteToHostException) {
            return FailureKind.CONNECTION;
        }
        return FailureKind.INTERNAL;
    }
}
