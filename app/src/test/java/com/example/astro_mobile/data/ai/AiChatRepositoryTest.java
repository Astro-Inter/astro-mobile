package com.example.astro_mobile.data.ai;

import static org.junit.Assert.*;

import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiChatRepositoryTest {
    private static final String SESSION = "c4dd8100-1122-3344-5566-77889900aabb";
    private static final String ANSWER = "{\"session_id\":\"" + SESSION
            + "\",\"resposta\":\"## Astro\\n**Olá**\",\"agentes_chamados\":[]}";

    @Test public void sendsFirebaseBearerMarkdownAndSession() throws Exception {
        Fixture f = new Fixture(200, ANSWER);
        Result r = f.send("  O que são NRs?  ", SESSION);
        assertNull(r.failure);
        assertEquals("Bearer firebase-id-token", f.headers.get(0));
        assertEquals("/chat/messages?markdown=true", f.paths.get(0));
        assertTrue(f.bodies.get(0).contains("\"session_id\":\"" + SESSION + "\""));
        assertTrue(f.bodies.get(0).contains("\"message\":\"O que são NRs?\""));
        assertEquals("## Astro\n**Olá**", r.response.getAnswer());
    }

    @Test public void firstMessageOmitsSession() throws Exception {
        Fixture f = new Fixture(200, ANSWER);
        f.send("Olá", null);
        assertFalse(f.bodies.get(0).contains("session_id"));
    }

    @Test public void refreshesTokenOnlyOnceAfterUnauthorized() throws Exception {
        Fixture f = new Fixture(401, "{}");
        assertEquals(ChatFailure.SESSION, f.send("Olá", null).failure);
        assertEquals(2, f.calls.get());
        assertEquals(2, f.refreshes.size());
        assertFalse(f.refreshes.get(0));
        assertTrue(f.refreshes.get(1));
    }

    @Test public void acceptsResponseAfterRefreshingToken() throws Exception {
        Fixture f = new Fixture(200, ANSWER);
        f.unauthorizedFirst = true;
        assertNotNull(f.send("Olá", null).response);
        assertEquals(2, f.calls.get());
        assertTrue(f.refreshes.get(1));
    }

    @Test public void rejectsEmptyAndOversizedMessagesBeforeReadingToken() throws Exception {
        Fixture f = new Fixture(200, ANSWER);
        assertEquals(ChatFailure.INVALID_MESSAGE, f.send("   ", null).failure);
        assertEquals(ChatFailure.INVALID_MESSAGE, f.send(new String(new char[4001]).replace('\0', 'a'), null).failure);
        assertEquals(0, f.calls.get());
        assertTrue(f.refreshes.isEmpty());
    }

    @Test public void rejectsIncompleteSuccessfulResponse() throws Exception {
        assertEquals(ChatFailure.INVALID_RESPONSE, new Fixture(200, "{}").send("Olá", null).failure);
    }

    @Test public void mapsPermissionRateLimitAndValidation() throws Exception {
        assertEquals(ChatFailure.FORBIDDEN, new Fixture(403, "{}").send("Olá", null).failure);
        assertEquals(ChatFailure.RATE_LIMIT, new Fixture(429, "{}").send("Olá", null).failure);
        assertEquals(ChatFailure.INVALID_MESSAGE, new Fixture(422, "{}").send("Olá", null).failure);
    }

    @Test public void neverRepeatsPostAfterTimeout() throws Exception {
        Fixture f = new Fixture(200, ANSWER);
        f.timeout = true;
        assertEquals(ChatFailure.TIMEOUT, f.send("Olá", null).failure);
        assertEquals(1, f.calls.get());
    }

    @Test public void cancellationWhileFetchingTokenPreventsHttpRequest() {
        Fixture f = new Fixture(200, ANSWER);
        final IdTokenProvider.Callback[] pending = new IdTokenProvider.Callback[1];
        AiChatRepository repository = new AiChatRepository(f.api,
                (force, callback) -> pending[0] = callback);
        Result result = new Result();
        AiChatRepository.Operation operation = repository.sendMessage("Olá", null, result);
        operation.cancel();
        pending[0].onToken("firebase-id-token");
        assertEquals(0, f.calls.get());
        assertEquals(1L, result.latch.getCount());
    }

    private static final class Result implements AiChatRepository.ResultCallback {
        final CountDownLatch latch = new CountDownLatch(1);
        ChatResponse response;
        ChatFailure failure;
        @Override public void onSuccess(ChatResponse response) {
            this.response = response;
            latch.countDown();
        }
        @Override public void onFailure(ChatFailure failure) {
            this.failure = failure;
            latch.countDown();
        }
    }

    private static final class Fixture {
        final AtomicInteger calls = new AtomicInteger();
        final List<String> headers = new ArrayList<>();
        final List<String> paths = new ArrayList<>();
        final List<String> bodies = new ArrayList<>();
        final List<Boolean> refreshes = new ArrayList<>();
        final AiChatApi api;
        boolean timeout;
        boolean unauthorizedFirst;

        Fixture(int code, String body) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                int count = calls.incrementAndGet();
                headers.add(chain.request().header("Authorization"));
                paths.add(chain.request().url().encodedPath() + "?" + chain.request().url().encodedQuery());
                Buffer buffer = new Buffer();
                chain.request().body().writeTo(buffer);
                bodies.add(buffer.readUtf8());
                if (timeout) throw new SocketTimeoutException("test timeout");
                return new Response.Builder().request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(unauthorizedFirst && count == 1 ? 401 : code).message("test")
                        .body(ResponseBody.create(MediaType.get("application/json"), body)).build();
            }).build();
            api = new Retrofit.Builder().baseUrl("https://example.com/")
                    .client(client).callbackExecutor(Runnable::run)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(AiChatApi.class);
        }

        Result send(String message, String session) throws Exception {
            Result result = new Result();
            new AiChatRepository(api, (force, callback) -> {
                refreshes.add(force);
                callback.onToken("firebase-id-token");
            }).sendMessage(message, session, result);
            assertTrue("callback não entregue", result.latch.await(5, TimeUnit.SECONDS));
            return result;
        }
    }
}
