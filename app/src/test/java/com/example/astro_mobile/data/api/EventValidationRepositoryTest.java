package com.example.astro_mobile.data.api;

import static org.junit.Assert.*;
import com.example.astro_mobile.data.api.dto.EventValidationData;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import org.junit.Test;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Verifica o contrato das ações de validação e os principais erros. */
public final class EventValidationRepositoryTest {
    private static final String DETAILS = "{\"success\":true,\"data\":{\"participantName\":\"Ana\",\"status\":\"PENDENTE\",\"evidences\":[{\"originalName\":\"foto.jpg\",\"mimeType\":\"image/jpeg\",\"url\":\"https://example.com/a\"}]}}";
    private static final String RESULT = "{\"success\":true,\"data\":{\"status\":\"CONCLUIDO\"}}";
    @Test public void loadsDetailsAndEvidenceUsingIdsAndToken() throws Exception {
        EventValidationRepository repo = repo(chain -> {
            assertEquals("GET", chain.request().method());
            assertEquals("/events/participant/12/collaborator-conclusion/34", chain.request().url().encodedPath());
            assertEquals("Bearer token", chain.request().header("Authorization"));
            return response(chain, 200, DETAILS);
        });
        Result<EventValidationData> result = new Result<>(); repo.load(34, 12, result); result.await();
        assertNull(result.failure); assertEquals("Ana", result.data.participantName);
        assertEquals("foto.jpg", result.data.evidences.get(0).originalName);
    }
    @Test public void sendsApprovalAndRejectionWithTheirCorrectBodies() throws Exception {
        for (boolean reject : new boolean[]{false, true}) {
            EventValidationRepository repo = repo(chain -> {
                assertEquals("PATCH", chain.request().method());
                assertEquals("/events/34/participants/12/" + (reject ? "rejection" : "collaborator-conclusion/approval"), chain.request().url().encodedPath());
                if (reject) { Buffer body = new Buffer(); chain.request().body().writeTo(body); assertEquals("{\"rejectionReason\":\"Precisa corrigir\"}", body.readUtf8()); }
                return response(chain, 200, RESULT);
            });
            Result<Object> result = new Result<>(); repo.decide(34, 12, reject ? "Precisa corrigir" : null, result); result.await();
            assertNull(result.failure); assertNotNull(result.data);
        }
    }
    @Test public void refreshesOnceAndDistinguishesTransportAndServerErrors() throws Exception {
        AtomicInteger count = new AtomicInteger();
        Result<Object> retry = new Result<>();
        repo(chain -> response(chain, count.getAndIncrement() == 0 ? 401 : 200, RESULT)).decide(34, 12, null, retry);
        retry.await(); assertNull(retry.failure); assertEquals(2, count.get());
        Result<Object> server = new Result<>(); repo(chain -> response(chain, 503, "{}")).decide(34, 12, null, server);
        server.await(); assertEquals(FailureKind.INTERNAL, server.failure);
        Result<Object> offline = new Result<>(); repo(chain -> { throw new UnknownHostException(); }).decide(34, 12, null, offline);
        offline.await(); assertEquals(FailureKind.CONNECTION, offline.failure);
    }
    private EventValidationRepository repo(Interceptor interceptor) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        AstroApi api = new Retrofit.Builder().baseUrl("https://example.com/").client(client)
                .callbackExecutor(Runnable::run).addConverterFactory(GsonConverterFactory.create()).build().create(AstroApi.class);
        IdTokenProvider tokens = (refresh, callback) -> callback.onToken(refresh ? "refreshed" : "token");
        return new EventValidationRepository(api, tokens);
    }
    private Response response(Interceptor.Chain chain, int code, String json) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
                .body(ResponseBody.create(MediaType.get("application/json"), json)).build();
    }
    private static final class Result<T> implements EventValidationRepository.Callback<T> {
        final CountDownLatch done = new CountDownLatch(1); T data; FailureKind failure;
        @Override public void onSuccess(T data) { this.data = data; done.countDown(); }
        @Override public void onFailure(FailureKind failure) { this.failure = failure; done.countDown(); }
        void await() throws InterruptedException { assertTrue(done.await(5, TimeUnit.SECONDS)); }
    }
}
