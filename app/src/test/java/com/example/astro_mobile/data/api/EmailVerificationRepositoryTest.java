package com.example.astro_mobile.data.api;

import static org.junit.Assert.assertEquals;

import com.example.astro_mobile.data.api.dto.VerifyEmailData;

import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EmailVerificationRepositoryTest {
    @Test
    public void notFoundIsInlineError() throws InterruptedException {
        Result result = request(404, "{\"success\":false,\"message\":\"Este e-mail não está cadastrado no Astro.\",\"data\":null,\"errors\":[\"Este e-mail não está cadastrado no Astro.\"]}", false);
        assertEquals(FailureKind.BUSINESS, result.kind);
        assertEquals("Este e-mail não está cadastrado no Astro.", result.message);
    }

    @Test
    public void validationFailureIsInlineError() throws InterruptedException {
        Result result = request(400, "{\"success\":false,\"message\":\"Falha de validação\",\"errors\":[\"E-mail inválido\"]}", false);
        assertEquals(FailureKind.BUSINESS, result.kind);
        assertEquals("E-mail inválido", result.message);
    }

    @Test
    public void businessFailureInSuccessStatusIsInlineError() throws InterruptedException {
        Result result = request(200, "{\"success\":false,\"message\":\"Conta indisponível\",\"data\":null}", false);
        assertEquals(FailureKind.BUSINESS, result.kind);
        assertEquals("Conta indisponível", result.message);
    }

    @Test
    public void serverErrorIsInternalEvenWithBody() throws InterruptedException {
        Result result = request(500, "{\"success\":false,\"message\":\"Erro interno do servidor\",\"data\":null}", false);
        assertEquals(FailureKind.INTERNAL, result.kind);
    }

    @Test
    public void networkFailureIsConnectionError() throws InterruptedException {
        Result result = request(200, "", true);
        assertEquals(FailureKind.CONNECTION, result.kind);
    }

    @Test
    public void unspecifiedIoFailureIsInternalError() {
        assertEquals(FailureKind.INTERNAL, ApiFailures.transport(new IOException("unspecified test failure")));
    }

    @Test
    public void malformedResponseIsInternalError() throws InterruptedException {
        Result result = request(200, "{}", false);
        assertEquals(FailureKind.INTERNAL, result.kind);
    }

    @Test
    public void validResponseKeepsTypeAndStatus() throws InterruptedException {
        Result result = request(200, "{\"success\":true,\"data\":{\"userType\":\"COLABORADOR\",\"userStatus\":\"ATIVO\"}}", false);
        assertEquals("COLABORADOR", result.data.getUserType());
        assertEquals("ATIVO", result.data.getUserStatus());
    }

    private Result request(int code, String body, boolean disconnect) throws InterruptedException {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            if (disconnect) {
                throw new UnknownHostException("offline test");
            }
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body(ResponseBody.create(MediaType.get("application/json"), body))
                    .build();
        }).build();
        AstroApi api = new Retrofit.Builder()
                .baseUrl("https://example.com/")
                .client(client)
                .callbackExecutor(Runnable::run)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(AstroApi.class);
        Result result = new Result();
        CountDownLatch latch = new CountDownLatch(1);
        new EmailVerificationRepository(api).verifyEmail("test@example.com",
                new EmailVerificationRepository.ResultCallback() {
                    @Override
                    public void onSuccess(VerifyEmailData data) {
                        result.data = data;
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(FailureKind kind,
                                          String message) {
                        result.kind = kind;
                        result.message = message;
                        latch.countDown();
                    }
                });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Repository callback timed out");
        }
        return result;
    }

    private static final class Result {
        VerifyEmailData data;
        FailureKind kind;
        String message;
    }
}
